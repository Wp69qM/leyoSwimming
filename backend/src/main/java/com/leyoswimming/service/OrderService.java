package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    List<AdminOrderListItemResponse> list =
        records.stream()
            .map(
                order -> {
                  User user = userMap.get(order.getUserId());
                  Coach coach =
                      order.getCoachId() == null ? null : coachMap.get(order.getCoachId());
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

    return new AdminOrderDetailResponse(
        order.getId(),
        order.getOrderNo(),
        order.getType(),
        order.getStatus(),
        order.getUserId(),
        user == null ? null : user.getName(),
        user == null ? null : user.getPhone(),
        order.getCoachId(),
        coach == null ? null : coach.getName(),
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
        null,
        order.getApprovedBy(),
        order.getApprovedAt(),
        order.getRefundedAt(),
        order.getCreatedAt(),
        timeline);
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
    if (refundAmount.compareTo(BigDecimal.ZERO) < 0
        || refundAmount.compareTo(calculatedAmount) > 0) {
      throw new BusinessException(
          ErrorCode.REFUND_AMOUNT_INVALID,
          "退款金额必须在 0 到 " + calculatedAmount + " 之间");
    }

    LocalDateTime now = LocalDateTime.now();

    Order updateOrder = new Order();
    updateOrder.setId(order.getId());
    updateOrder.setPaidAmount(refundAmount);
    updateOrder.setStatus(OrderStatus.REFUND_PROCESSING.getValue());
    updateOrder.setApprovedBy(adminId);
    updateOrder.setApprovedAt(now);
    orderMapper.updateById(updateOrder);

    LambdaQueryWrapper<RefundRecord> recordWrapper = new LambdaQueryWrapper<>();
    recordWrapper.eq(RefundRecord::getOrderId, order.getId()).last("LIMIT 1");
    RefundRecord refundRecord = refundRecordMapper.selectOne(recordWrapper);
    if (refundRecord != null) {
      RefundRecord updateRecord = new RefundRecord();
      updateRecord.setId(refundRecord.getId());
      updateRecord.setRefundAmount(refundAmount);
      updateRecord.setStatus(REFUND_RECORD_STATUS_APPROVED);
      refundRecordMapper.updateById(updateRecord);
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
        "Refund approved: adminId={}, orderId={}, amount={}, calculated={}",
        adminId,
        order.getId(),
        refundAmount,
        calculatedAmount);

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

    Order updateOrder = new Order();
    updateOrder.setId(order.getId());
    updateOrder.setStatus(OrderStatus.REJECTED.getValue());
    updateOrder.setRejectedReason(request.rejectedReason());
    updateOrder.setApprovedBy(adminId);
    updateOrder.setApprovedAt(now);
    orderMapper.updateById(updateOrder);

    LambdaQueryWrapper<RefundRecord> recordWrapper = new LambdaQueryWrapper<>();
    recordWrapper.eq(RefundRecord::getOrderId, order.getId()).last("LIMIT 1");
    RefundRecord refundRecord = refundRecordMapper.selectOne(recordWrapper);
    if (refundRecord != null) {
      RefundRecord updateRecord = new RefundRecord();
      updateRecord.setId(refundRecord.getId());
      updateRecord.setStatus(REFUND_RECORD_STATUS_REJECTED);
      refundRecordMapper.updateById(updateRecord);
    }

    if (order.getPackageId() != null) {
      CoursePackage coursePackage = packageMapper.selectById(order.getPackageId());
      if (coursePackage != null
          && "frozen".equals(coursePackage.getStatus())
          && REFUND_FROZEN_REASON.equals(coursePackage.getFrozenReason())) {
        UpdateWrapper<CoursePackage> packageWrapper = new UpdateWrapper<>();
        packageWrapper
            .eq("id", coursePackage.getId())
            .set("status", "active")
            .set("frozen_reason", null);
        packageMapper.update(packageWrapper);
      }
    }

    log.info("Refund rejected: adminId={}, orderId={}, reason={}", adminId, order.getId(), request.rejectedReason());
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
}
