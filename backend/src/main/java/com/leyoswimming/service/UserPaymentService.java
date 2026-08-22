package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.OrderPayRequest;
import com.leyoswimming.dto.request.PaymentMockCallbackRequest;
import com.leyoswimming.dto.response.OrderPayResponse;
import com.leyoswimming.dto.response.PaymentMockCallbackResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.Payment;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.PaymentMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPaymentService {

  private static final String CHANNEL_WECHAT = "wechat";
  private static final String CHANNEL_ALIPAY = "alipay";
  private static final String PAYMENT_STATUS_PENDING = "pending";
  private static final String PAYMENT_STATUS_SUCCESS = "success";
  private static final String PAYMENT_STATUS_FAILED = "failed";
  private static final String PACKAGE_STATUS_ACTIVE = "active";
  private static final String PACKAGE_NO_PREFIX = "P";
  private static final String LOCK_RESOURCE_PAYMENT_PAY = "payment:order";
  private static final String LOCK_RESOURCE_PAYMENT_CALLBACK = "payment:callback";
  private static final Duration LOCK_TTL_PAYMENT = Duration.ofSeconds(30);

  private final OrderMapper orderMapper;
  private final PaymentMapper paymentMapper;
  private final PackageMapper packageMapper;
  private final DistributedLockHelper lockHelper;

  @Transactional
  public OrderPayResponse pay(Long userId, OrderPayRequest request) {
    String channel = resolveChannel(request.channel());
    if (channel == null) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付渠道错误");
    }

    String lockIdentifier = "user:" + userId + ":order:" + request.orderId();
    LockToken lock = lockHelper.lock(LOCK_RESOURCE_PAYMENT_PAY, lockIdentifier, LOCK_TTL_PAYMENT);
    try {
      Order order = orderMapper.selectById(request.orderId());
      if (order == null || !userId.equals(order.getUserId())) {
        throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
      }
      if (!OrderType.PURCHASE.getValue().equals(order.getType())) {
        throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "非购买订单");
      }
      if (!OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
        throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单不在待支付状态");
      }
      if (order.getExpireAt() != null && LocalDateTime.now().isAfter(order.getExpireAt())) {
        throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单已过期");
      }

      LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
      paymentWrapper
          .eq(Payment::getOrderId, order.getId())
          .eq(Payment::getStatus, PAYMENT_STATUS_PENDING)
          .last("LIMIT 1");
      Payment existingPayment = paymentMapper.selectOne(paymentWrapper);
      if (existingPayment != null) {
        return new OrderPayResponse(
            existingPayment.getId(), existingPayment.getChannelTradeNo(), PAYMENT_STATUS_PENDING);
      }

      Payment payment = new Payment();
      payment.setOrderId(order.getId());
      payment.setUserId(userId);
      payment.setChannel(channel);
      payment.setAmount(order.getOriginalAmount());
      payment.setStatus(PAYMENT_STATUS_PENDING);
      payment.setChannelTradeNo(generateMockTradeNo(channel));
      paymentMapper.insert(payment);

      log.info("Payment initiated: orderId={}, paymentId={}, channel={}", order.getId(), payment.getId(), channel);
      return new OrderPayResponse(payment.getId(), payment.getChannelTradeNo(), PAYMENT_STATUS_PENDING);
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  @Transactional
  public PaymentMockCallbackResponse mockCallback(PaymentMockCallbackRequest request) {
    String lockIdentifier = "order:" + request.orderId() + ":trade:" + request.channelTradeNo();
    LockToken lock = lockHelper.lock(LOCK_RESOURCE_PAYMENT_CALLBACK, lockIdentifier, LOCK_TTL_PAYMENT);
    try {
      Payment payment = findPaymentByTradeNo(request.orderId(), request.channelTradeNo());
      if (payment == null) {
        log.warn("Mock callback payment not found: orderId={}, tradeNo={}", request.orderId(), request.channelTradeNo());
        return PaymentMockCallbackResponse.success();
      }

      if (!PAYMENT_STATUS_PENDING.equals(payment.getStatus())) {
        return PaymentMockCallbackResponse.success();
      }

      if (request.amount().compareTo(payment.getAmount()) != 0) {
        log.warn("Mock callback amount mismatch: paymentAmount={}, callbackAmount={}", payment.getAmount(), request.amount());
        return PaymentMockCallbackResponse.success();
      }

      Order order = orderMapper.selectById(payment.getOrderId());
      if (order == null) {
        log.warn("Mock callback order not found: orderId={}", payment.getOrderId());
        return PaymentMockCallbackResponse.success();
      }
      if (!OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
        log.warn("Mock callback order not in pending payment: orderId={}, status={}", order.getId(), order.getStatus());
        return PaymentMockCallbackResponse.success();
      }

      if (Boolean.FALSE.equals(request.success())) {
        markPaymentFailed(payment.getId());
        return PaymentMockCallbackResponse.success();
      }

      processSuccessfulPayment(payment, order);
      return PaymentMockCallbackResponse.success();
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  private Payment findPaymentByTradeNo(Long orderId, String channelTradeNo) {
    LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(Payment::getOrderId, orderId)
        .eq(Payment::getChannelTradeNo, channelTradeNo)
        .eq(Payment::getStatus, PAYMENT_STATUS_PENDING)
        .last("LIMIT 1");
    return paymentMapper.selectOne(wrapper);
  }

  private void markPaymentFailed(Long paymentId) {
    Payment updatePayment = new Payment();
    updatePayment.setStatus(PAYMENT_STATUS_FAILED);
    UpdateWrapper<Payment> wrapper = new UpdateWrapper<>();
    wrapper.eq("id", paymentId).eq("status", PAYMENT_STATUS_PENDING);
    paymentMapper.update(updatePayment, wrapper);
  }

  private void processSuccessfulPayment(Payment payment, Order order) {
    LocalDateTime now = LocalDateTime.now();

    Payment updatePayment = new Payment();
    updatePayment.setId(payment.getId());
    updatePayment.setStatus(PAYMENT_STATUS_SUCCESS);
    updatePayment.setPaidAt(now);
    paymentMapper.updateById(updatePayment);

    CoursePackage coursePackage = buildPackageFromOrder(order);
    coursePackage.setOrderId(order.getId());
    coursePackage.setPaidAmount(payment.getAmount());
    coursePackage.setStatus(PACKAGE_STATUS_ACTIVE);
    coursePackage.setExpireAt(now.plusDays(order.getValidDays() == null ? 0 : order.getValidDays()));
    packageMapper.insert(coursePackage);

    Order updateOrder = new Order();
    updateOrder.setId(order.getId());
    updateOrder.setStatus(OrderStatus.PAID.getValue());
    updateOrder.setPaidAmount(payment.getAmount());
    updateOrder.setPaidAt(now);
    updateOrder.setPaymentMethod(payment.getChannel());
    updateOrder.setChannelTradeNo(payment.getChannelTradeNo());
    updateOrder.setPackageId(coursePackage.getId());
    orderMapper.updateById(updateOrder);

    log.info("Payment callback success: orderId={}, packageId={}, amount={}", order.getId(), coursePackage.getId(), payment.getAmount());
  }

  private CoursePackage buildPackageFromOrder(Order order) {
    CoursePackage pkg = new CoursePackage();
    pkg.setPackageNo(OrderNoGenerator.generateOrderNo(PACKAGE_NO_PREFIX));
    pkg.setUserId(order.getUserId());
    pkg.setCoachId(order.getCoachId());
    pkg.setPackageMode(order.getPackageMode());
    pkg.setPackageName(order.getPackageName());
    pkg.setCoachName(order.getCoachName());
    pkg.setTeachingType(order.getTeachingType());
    pkg.setStrokeIds(order.getStrokeIds());
    pkg.setTotalHours(order.getTotalHours());
    pkg.setDurationMinutes(order.getDurationMinutes());
    pkg.setValidDays(order.getValidDays());
    pkg.setOriginalPrice(order.getOriginalAmount());
    pkg.setRefundEnabled(order.getRefundEnabled());
    pkg.setRefundRatio(order.getRefundRatio());
    pkg.setRefundValidDays(order.getRefundValidDays());
    pkg.setAvailableCount(order.getTotalHours());
    pkg.setConsumedCount(0);
    pkg.setReservedCount(0);
    pkg.setPricePerHour(calculatePricePerHour(order));
    return pkg;
  }

  private BigDecimal calculatePricePerHour(Order order) {
    if (order.getTotalHours() == null || order.getTotalHours() <= 0) {
      return BigDecimal.ZERO;
    }
    if (PackageMode.EXPERIENCE.getValue().equals(order.getPackageMode())) {
      return BigDecimal.ZERO;
    }
    return order.getOriginalAmount()
        .divide(BigDecimal.valueOf(order.getTotalHours()), 2, java.math.RoundingMode.HALF_UP);
  }

  private String resolveChannel(Integer channel) {
    if (channel == null) {
      return null;
    }
    return switch (channel) {
      case 0 -> CHANNEL_WECHAT;
      case 1 -> CHANNEL_ALIPAY;
      default -> null;
    };
  }

  private String generateMockTradeNo(String channel) {
    return "MOCK-" + channel.toUpperCase() + "-" + UUID.randomUUID();
  }
}
