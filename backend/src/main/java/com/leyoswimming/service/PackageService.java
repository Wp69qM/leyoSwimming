package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

  private static final String REFUND_ORDER_PREFIX = "R";
  private static final String REFUND_FROZEN_REASON = "refund_pending";

  private final PackageMapper packageMapper;
  private final OrderMapper orderMapper;
  private final RefundRecordMapper refundRecordMapper;

  @Transactional
  public UserPackageRefundResponse requestRefund(Long userId, UserPackageRefundRequest request) {
    CoursePackage coursePackage = packageMapper.selectById(request.packageId());
    if (coursePackage == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }
    if (!userId.equals(coursePackage.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!"active".equals(coursePackage.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_STATUS_NOT_ALLOWED, "只有 active 状态的套餐可申请退款");
    }
    if (Boolean.FALSE.equals(coursePackage.getRefundEnabled())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "该套餐不允许退款");
    }
    if (coursePackage.getRefundValidDays() != null
        && coursePackage.getRefundValidDays() > 0
        && coursePackage.getCreatedAt() != null) {
      LocalDateTime deadline =
          coursePackage.getCreatedAt().plusDays(coursePackage.getRefundValidDays());
      if (LocalDateTime.now().isAfter(deadline)) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "已超过退款有效期");
      }
    }

    LambdaQueryWrapper<Order> pendingRefundWrapper = new LambdaQueryWrapper<>();
    pendingRefundWrapper
        .eq(Order::getPackageId, request.packageId())
        .eq(Order::getType, OrderType.REFUND.getValue())
        .eq(Order::getStatus, OrderStatus.REFUND_PENDING.getValue());
    if (orderMapper.selectCount(pendingRefundWrapper) > 0) {
      throw new BusinessException(ErrorCode.REFUND_PENDING_EXISTS);
    }

    LambdaQueryWrapper<Order> purchaseWrapper = new LambdaQueryWrapper<>();
    purchaseWrapper
        .eq(Order::getPackageId, request.packageId())
        .eq(Order::getType, OrderType.PURCHASE.getValue())
        .eq(Order::getStatus, OrderStatus.PAID.getValue())
        .orderByDesc(Order::getId)
        .last("LIMIT 1");
    Order purchaseOrder = orderMapper.selectOne(purchaseWrapper);
    if (purchaseOrder == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "未找到对应购买订单");
    }

    BigDecimal refundAmount = calculateRefundAmount(coursePackage);
    if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "可退金额必须大于 0");
    }

    Order refundOrder = new Order();
    refundOrder.setOrderNo(OrderNoGenerator.generateOrderNo(REFUND_ORDER_PREFIX));
    refundOrder.setType(OrderType.REFUND.getValue());
    refundOrder.setStatus(OrderStatus.REFUND_PENDING.getValue());
    refundOrder.setUserId(userId);
    refundOrder.setCoachId(coursePackage.getCoachId());
    refundOrder.setPackageId(coursePackage.getId());
    refundOrder.setPurchaseOrderId(purchaseOrder.getId());
    refundOrder.setOriginalAmount(refundAmount);
    refundOrder.setPaidAmount(refundAmount);
    refundOrder.setReason(request.reason());
    orderMapper.insert(refundOrder);

    RefundRecord refundRecord = new RefundRecord();
    refundRecord.setPackageId(coursePackage.getId());
    refundRecord.setOrderId(refundOrder.getId());
    refundRecord.setRefundAmount(refundAmount);
    refundRecord.setReason(request.reason());
    refundRecord.setStatus(0);
    refundRecordMapper.insert(refundRecord);

    CoursePackage updatePackage = new CoursePackage();
    updatePackage.setId(coursePackage.getId());
    updatePackage.setStatus("frozen");
    updatePackage.setFrozenReason(REFUND_FROZEN_REASON);
    packageMapper.updateById(updatePackage);

    log.info(
        "Refund requested: userId={}, packageId={}, orderId={}, amount={}",
        userId,
        coursePackage.getId(),
        refundOrder.getId(),
        refundAmount);

    return new UserPackageRefundResponse(refundOrder.getId(), refundOrder.getOrderNo(), refundAmount);
  }

  public BigDecimal calculateRefundAmount(CoursePackage coursePackage) {
    BigDecimal paidAmount =
        coursePackage.getPaidAmount() == null ? BigDecimal.ZERO : coursePackage.getPaidAmount();
    int totalHours =
        coursePackage.getTotalHours() == null ? 0 : coursePackage.getTotalHours();
    int consumedCount =
        coursePackage.getConsumedCount() == null ? 0 : coursePackage.getConsumedCount();
    BigDecimal refundRatio =
        coursePackage.getRefundRatio() == null
            ? BigDecimal.ONE
            : coursePackage.getRefundRatio();

    if (totalHours <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal remainingRatio =
        BigDecimal.valueOf(totalHours - consumedCount)
            .divide(BigDecimal.valueOf(totalHours), 4, RoundingMode.HALF_UP);
    return paidAmount
        .multiply(remainingRatio)
        .multiply(refundRatio)
        .setScale(2, RoundingMode.HALF_UP);
  }
}
