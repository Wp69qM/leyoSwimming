package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminOrderListRequest;
import com.leyoswimming.dto.request.AdminOrderRefundApproveRequest;
import com.leyoswimming.dto.request.AdminOrderRefundRejectRequest;
import com.leyoswimming.dto.response.AdminOrderDetailResponse;
import com.leyoswimming.dto.response.AdminOrderListItemResponse;
import com.leyoswimming.dto.response.AdminOrderListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.entity.RefundTransaction;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.RefundTransactionMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.OrderNoGenerator;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private static final String REFUND_TRANSACTION_PREFIX = "RT";
  private static final String REFUND_FROZEN_REASON = "refund_pending";
  private static final int REFUND_RECORD_STATUS_PENDING = 0;
  private static final int REFUND_RECORD_STATUS_APPROVED = 1;
  private static final int REFUND_RECORD_STATUS_REJECTED = 2;

  private final OrderMapper orderMapper;
  private final PackageMapper packageMapper;
  private final RefundRecordMapper refundRecordMapper;
  private final RefundTransactionMapper refundTransactionMapper;
  private final UserMapper userMapper;
  private final CoachMapper coachMapper;
  private final PackageService packageService;
  private final MockRefundChannelService mockRefundChannelService;
  private final AdminPermissionHelper permissionHelper;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional(readOnly = true)
  public AdminOrderListResponse list(Long adminId, AdminOrderListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_ORDER_READ);

    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.type())) {
      wrapper.eq(Order::getType, request.type().trim().toLowerCase());
    }
    if (StringUtils.isNotBlank(request.status())) {
      wrapper.eq(Order::getStatus, request.status().trim().toLowerCase());
    }
    if (StringUtils.isNotBlank(request.paymentMethod())) {
      wrapper.eq(Order::getPaymentMethod, request.paymentMethod().trim().toLowerCase());
    }
    if (StringUtils.isNotBlank(request.startDate())) {
      LocalDate startDate = LocalDate.parse(request.startDate().trim());
      wrapper.ge(Order::getCreatedAt, LocalDateTime.of(startDate, LocalTime.MIN));
    }
    if (StringUtils.isNotBlank(request.endDate())) {
      LocalDate endDate = LocalDate.parse(request.endDate().trim());
      wrapper.le(Order::getCreatedAt, LocalDateTime.of(endDate, LocalTime.MAX));
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      wrapper.like(Order::getOrderNo, request.keyword().trim());
    }
    wrapper.orderByDesc(Order::getCreatedAt);

    Page<Order> page = new Page<>(request.page(), request.pageSize());
    Page<Order> result = orderMapper.selectPage(page, wrapper);

    List<Order> records = result.getRecords();
    if (records.isEmpty()) {
      return new AdminOrderListResponse(List.of(), result.getTotal(), request.page(), request.pageSize());
    }

    Map<Long, User> userMap =
        userMapper
            .selectBatchIds(records.stream().map(Order::getUserId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Long, Coach> coachMap =
        coachMapper
            .selectBatchIds(
                records.stream().map(Order::getCoachId).filter(Objects::nonNull).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Coach::getId, Function.identity()));

    List<Long> packageIds =
        records.stream()
            .map(Order::getPackageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<Long, CoursePackage> packageMap =
        packageIds.isEmpty()
            ? Map.of()
            : packageMapper.selectBatchIds(packageIds).stream()
                .collect(Collectors.toMap(CoursePackage::getId, Function.identity()));

    List<AdminOrderListItemResponse> list =
        records.stream()
            .map(
                order -> {
                  User user = userMap.get(order.getUserId());
                  Coach coach =
                      order.getCoachId() == null ? null : coachMap.get(order.getCoachId());
                  CoursePackage coursePackage =
                      order.getPackageId() == null ? null : packageMap.get(order.getPackageId());
                  BigDecimal calculatedRefundAmount = null;
                  if (OrderType.REFUND.getValue().equals(order.getType())
                      && coursePackage != null) {
                    calculatedRefundAmount = packageService.calculateRefundAmount(coursePackage);
                  }
                  return new AdminOrderListItemResponse(
                      order.getId(),
                      order.getOrderNo(),
                      order.getType(),
                      order.getStatus(),
                      order.getUserId(),
                      user == null ? null : user.getName(),
                      order.getCoachId(),
                      coach == null ? null : coach.getName(),
                      order.getPackageId(),
                      order.getOriginalAmount(),
                      order.getDiscountAmount(),
                      order.getPaidAmount(),
                      calculatedRefundAmount,
                      order.getPaymentMethod(),
                      order.getReason(),
                      order.getCreatedAt());
                })
            .toList();

    return new AdminOrderListResponse(list, result.getTotal(), request.page(), request.pageSize());
  }

  @Transactional(readOnly = true)
  public AdminOrderDetailResponse detail(Long adminId, Long orderId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_ORDER_READ);

    Order order = orderMapper.selectById(orderId);
    if (order == null) {
      throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }

    User user = userMapper.selectById(order.getUserId());
    Coach coach = order.getCoachId() == null ? null : coachMapper.selectById(order.getCoachId());
    CoursePackage coursePackage =
        order.getPackageId() == null ? null : packageMapper.selectById(order.getPackageId());
    Order purchaseOrder =
        order.getPurchaseOrderId() == null ? null : orderMapper.selectById(order.getPurchaseOrderId());

    BigDecimal calculatedRefundAmount = null;
    if (OrderType.REFUND.getValue().equals(order.getType()) && coursePackage != null) {
      calculatedRefundAmount = packageService.calculateRefundAmount(coursePackage);
    }

    List<AdminOrderDetailResponse.OrderStatusLog> timeline = buildStatusTimeline(order);

    String teachingType = order.getTeachingType();
    if (teachingType == null && coursePackage != null) {
      teachingType = coursePackage.getTeachingType();
    }

    AdminOrderDetailResponse.PackageSnapshot packageSnapshot = null;
    if (coursePackage != null) {
      packageSnapshot =
          new AdminOrderDetailResponse.PackageSnapshot(
              coursePackage.getId(),
              coursePackage.getPackageNo(),
              coursePackage.getStatus(),
              coursePackage.getTotalHours(),
              coursePackage.getAvailableCount(),
              coursePackage.getExpireAt());
    }

    return new AdminOrderDetailResponse(
        order.getId(),
        order.getOrderNo(),
        order.getType(),
        order.getStatus(),
        order.getUserId(),
        user == null ? null : user.getName(),
        user == null ? null : decryptPhone(user.getPhone(), user.getId()),
        order.getCoachId(),
        coach == null ? null : coach.getName(),
        teachingType,
        order.getPackageId(),
        order.getPurchaseOrderId(),
        purchaseOrder == null ? null : purchaseOrder.getOrderNo(),
        order.getOriginalAmount(),
        order.getDiscountAmount(),
        order.getPaidAmount(),
        calculatedRefundAmount,
        order.getPaymentMethod(),
        order.getChannelTradeNo(),
        order.getReason(),
        order.getRejectedReason(),
        order.getAdjustReason(),
        order.getApprovedBy(),
        order.getApprovedAt(),
        order.getRefundedAt(),
        order.getPaidAt(),
        order.getCreatedAt(),
        timeline,
        packageSnapshot);
  }

  @Transactional
  public void approveRefund(Long adminId, AdminOrderRefundApproveRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_ORDER_WRITE);

    Order order = orderMapper.selectById(request.orderId());
    if (order == null) {
      throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }
    if (!OrderType.REFUND.getValue().equals(order.getType())) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "非退款订单");
    }
    if (!OrderStatus.REFUND_PENDING.getValue().equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单不在退款审批中状态");
    }

    CoursePackage coursePackage = packageMapper.selectById(order.getPackageId());
    if (coursePackage == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }

    BigDecimal calculatedAmount = packageService.calculateRefundAmount(coursePackage);
    BigDecimal refundAmount = request.refundAmount().setScale(2, RoundingMode.HALF_UP);
    if (refundAmount.compareTo(BigDecimal.ZERO) <= 0
        || refundAmount.compareTo(calculatedAmount) > 0) {
      throw new BusinessException(
          ErrorCode.REFUND_AMOUNT_INVALID,
          "退款金额超出可退范围");
    }

    LocalDateTime now = LocalDateTime.now();

    LambdaUpdateWrapper<Order> orderWrapper = new LambdaUpdateWrapper<>();
    orderWrapper
        .eq(Order::getId, order.getId())
        .eq(Order::getStatus, OrderStatus.REFUND_PENDING.getValue())
        .set(Order::getPaidAmount, refundAmount)
        .set(Order::getAdjustReason, request.adjustReason())
        .set(Order::getStatus, OrderStatus.REFUND_PROCESSING.getValue())
        .set(Order::getApprovedBy, adminId)
        .set(Order::getApprovedAt, now);
    int orderUpdated = orderMapper.update(orderWrapper);
    if (orderUpdated == 0) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单状态已变更，请刷新后重试");
    }

    LambdaQueryWrapper<RefundRecord> recordWrapper = new LambdaQueryWrapper<>();
    recordWrapper
        .eq(RefundRecord::getOrderId, order.getId())
        .eq(RefundRecord::getStatus, REFUND_RECORD_STATUS_PENDING)
        .last("LIMIT 1");
    RefundRecord refundRecord = refundRecordMapper.selectOne(recordWrapper);
    if (refundRecord != null) {
      LambdaUpdateWrapper<RefundRecord> updateRecordWrapper = new LambdaUpdateWrapper<>();
      updateRecordWrapper
          .eq(RefundRecord::getId, refundRecord.getId())
          .eq(RefundRecord::getStatus, REFUND_RECORD_STATUS_PENDING)
          .set(RefundRecord::getRefundAmount, refundAmount)
          .set(RefundRecord::getStatus, REFUND_RECORD_STATUS_APPROVED);
      refundRecordMapper.update(updateRecordWrapper);
    }

    RefundTransaction transaction = new RefundTransaction();
    transaction.setOrderId(order.getId());
    transaction.setRefundRecordId(refundRecord == null ? null : refundRecord.getId());
    transaction.setChannel(order.getPaymentMethod() == null ? "mock" : order.getPaymentMethod());
    transaction.setChannelRefundNo(OrderNoGenerator.generateOrderNo(REFUND_TRANSACTION_PREFIX));
    transaction.setAmount(refundAmount);
    transaction.setStatus("pending");
    refundTransactionMapper.insert(transaction);

    log.info(
        "Refund approved: adminId={}, orderId={}",
        adminId,
        order.getId());

    mockRefundChannelService.notifyRefundSuccess(transaction.getChannelRefundNo());
  }

  @Transactional
  public void rejectRefund(Long adminId, AdminOrderRefundRejectRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_ORDER_WRITE);

    Order order = orderMapper.selectById(request.orderId());
    if (order == null) {
      throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }
    if (!OrderType.REFUND.getValue().equals(order.getType())) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "非退款订单");
    }
    if (!OrderStatus.REFUND_PENDING.getValue().equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单不在退款审批中状态");
    }

    LocalDateTime now = LocalDateTime.now();

    LambdaUpdateWrapper<Order> orderWrapper = new LambdaUpdateWrapper<>();
    orderWrapper
        .eq(Order::getId, order.getId())
        .eq(Order::getStatus, OrderStatus.REFUND_PENDING.getValue())
        .set(Order::getStatus, OrderStatus.REJECTED.getValue())
        .set(Order::getRejectedReason, request.rejectedReason())
        .set(Order::getApprovedBy, adminId)
        .set(Order::getApprovedAt, now);
    int orderUpdated = orderMapper.update(orderWrapper);
    if (orderUpdated == 0) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_NOT_ALLOWED, "订单状态已变更，请刷新后重试");
    }

    LambdaQueryWrapper<RefundRecord> recordWrapper = new LambdaQueryWrapper<>();
    recordWrapper
        .eq(RefundRecord::getOrderId, order.getId())
        .eq(RefundRecord::getStatus, REFUND_RECORD_STATUS_PENDING)
        .last("LIMIT 1");
    RefundRecord refundRecord = refundRecordMapper.selectOne(recordWrapper);
    if (refundRecord != null) {
      LambdaUpdateWrapper<RefundRecord> updateRecordWrapper = new LambdaUpdateWrapper<>();
      updateRecordWrapper
          .eq(RefundRecord::getId, refundRecord.getId())
          .eq(RefundRecord::getStatus, REFUND_RECORD_STATUS_PENDING)
          .set(RefundRecord::getStatus, REFUND_RECORD_STATUS_REJECTED);
      refundRecordMapper.update(updateRecordWrapper);
    }

    if (order.getPackageId() != null) {
      CoursePackage coursePackage = packageMapper.selectById(order.getPackageId());
      if (coursePackage != null
          && "frozen".equals(coursePackage.getStatus())
          && REFUND_FROZEN_REASON.equals(coursePackage.getFrozenReason())) {
        LambdaUpdateWrapper<CoursePackage> packageWrapper = new LambdaUpdateWrapper<>();
        packageWrapper
            .eq(CoursePackage::getId, coursePackage.getId())
            .set(CoursePackage::getStatus, "active")
            .set(CoursePackage::getFrozenReason, null);
        packageMapper.update(packageWrapper);
      }
    }

    log.info("Refund rejected: adminId={}, orderId={}", adminId, order.getId());
  }

  private List<AdminOrderDetailResponse.OrderStatusLog> buildStatusTimeline(Order order) {
    List<AdminOrderDetailResponse.OrderStatusLog> timeline = new ArrayList<>();
    timeline.add(
        new AdminOrderDetailResponse.OrderStatusLog(
            "created", order.getCreatedAt(), "订单创建"));

    if (order.getApprovedAt() != null) {
      if (OrderStatus.REFUND_PROCESSING.getValue().equals(order.getStatus())
          || OrderStatus.REFUNDED.getValue().equals(order.getStatus())) {
        timeline.add(
            new AdminOrderDetailResponse.OrderStatusLog(
                "approved", order.getApprovedAt(), "退款审批通过"));
      } else if (OrderStatus.REJECTED.getValue().equals(order.getStatus())) {
        timeline.add(
            new AdminOrderDetailResponse.OrderStatusLog(
                "rejected", order.getApprovedAt(), "退款审批驳回"));
      }
    }

    if (order.getRefundedAt() != null) {
      timeline.add(
          new AdminOrderDetailResponse.OrderStatusLog(
              "refunded", order.getRefundedAt(), "退款到账"));
    }

    return timeline;
  }

  private String decryptPhone(String encrypted, Long userId) {
    if (encrypted == null) {
      return null;
    }
    try {
      return phoneEncryptor.decrypt(encrypted);
    } catch (Exception e) {
      log.error("Failed to decrypt phone for userId={}", userId, e);
      return null;
    }
  }
}
