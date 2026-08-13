package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminOrderListRequest;
import com.leyoswimming.dto.request.AdminOrderRefundApproveRequest;
import com.leyoswimming.dto.request.AdminOrderRefundRejectRequest;
import com.leyoswimming.dto.response.AdminOrderDetailResponse;
import com.leyoswimming.dto.response.AdminOrderListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundRecord;
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
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderMapper orderMapper;
  @Mock private PackageMapper packageMapper;
  @Mock private RefundRecordMapper refundRecordMapper;
  @Mock private RefundTransactionMapper refundTransactionMapper;
  @Mock private UserMapper userMapper;
  @Mock private CoachMapper coachMapper;
  @Mock private PackageService packageService;
  @Mock private MockRefundChannelService mockRefundChannelService;
  @Mock private AdminPermissionHelper permissionHelper;

  @InjectMocks private OrderService orderService;

  @Test
  @DisplayName("approveRefund 通过并允许管理员调低退款金额")
  void approveRefund_withLowerAmount_updatesOrderAndTransaction() {
    Long orderId = 10L;
    Order order = refundOrder(orderId, 100L, 5L);
    CoursePackage pkg = activePackage(5L, BigDecimal.valueOf(1000), 10, 2, BigDecimal.ONE);

    when(orderMapper.selectById(orderId)).thenReturn(order);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(packageService.calculateRefundAmount(pkg)).thenReturn(BigDecimal.valueOf(800));
    RefundRecord record = new RefundRecord();
    record.setId(20L);
    when(refundRecordMapper.selectOne(any())).thenReturn(record);

    orderService.approveRefund(
        1L, new AdminOrderRefundApproveRequest(orderId, BigDecimal.valueOf(600), "协商一致"));

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderMapper).updateById(orderCaptor.capture());
    Order updatedOrder = orderCaptor.getValue();
    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REFUND_PROCESSING.getValue());
    assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(600));
    assertThat(updatedOrder.getApprovedBy()).isEqualTo(1L);

    ArgumentCaptor<RefundRecord> recordCaptor = ArgumentCaptor.forClass(RefundRecord.class);
    verify(refundRecordMapper).updateById(recordCaptor.capture());
    assertThat(recordCaptor.getValue().getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(600));

    verify(refundTransactionMapper).insert(any(com.leyoswimming.entity.RefundTransaction.class));
    verify(mockRefundChannelService).notifyRefundSuccess(any());
  }

  @Test
  @DisplayName("approveRefund 金额超过可退金额时抛出异常")
  void approveRefund_amountExceedsLimit_throws() {
    Long orderId = 10L;
    Order order = refundOrder(orderId, 100L, 5L);
    CoursePackage pkg = activePackage(5L, BigDecimal.valueOf(1000), 10, 2, BigDecimal.ONE);

    when(orderMapper.selectById(orderId)).thenReturn(order);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(packageService.calculateRefundAmount(pkg)).thenReturn(BigDecimal.valueOf(800));

    assertThatThrownBy(
            () ->
                orderService.approveRefund(
                    1L,
                    new AdminOrderRefundApproveRequest(orderId, BigDecimal.valueOf(900), null)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REFUND_AMOUNT_INVALID));
  }

  @Test
  @DisplayName("rejectRefund 驳回后恢复套餐为 active")
  void rejectRefund_validOrder_restoresPackageActive() {
    Long orderId = 10L;
    Order order = refundOrder(orderId, 100L, 5L);
    CoursePackage pkg = frozenPackage(5L, "refund_pending");

    when(orderMapper.selectById(orderId)).thenReturn(order);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    RefundRecord record = new RefundRecord();
    record.setId(20L);
    when(refundRecordMapper.selectOne(any())).thenReturn(record);

    orderService.rejectRefund(
        1L, new AdminOrderRefundRejectRequest(orderId, "资料不足"));

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderMapper).updateById(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.REJECTED.getValue());
    assertThat(orderCaptor.getValue().getRejectedReason()).isEqualTo("资料不足");

    ArgumentCaptor<UpdateWrapper<CoursePackage>> wrapperCaptor =
        ArgumentCaptor.forClass(UpdateWrapper.class);
    verify(packageMapper).update(wrapperCaptor.capture());
    UpdateWrapper<CoursePackage> captured = wrapperCaptor.getValue();
    assertThat(captured).isNotNull();
  }

  @Test
  @DisplayName("detail 返回退款订单详情与可退金额")
  void detail_refundOrder_returnsCalculatedRefundAmount() {
    Long orderId = 10L;
    Order order = refundOrder(orderId, 100L, 5L);
    order.setPurchaseOrderId(20L);
    CoursePackage pkg = activePackage(5L, BigDecimal.valueOf(1000), 10, 2, BigDecimal.ONE);
    User user = new User();
    user.setId(100L);
    user.setName("学员");
    Coach coach = new Coach();
    coach.setId(1L);
    coach.setName("教练");
    Order purchaseOrder = new Order();
    purchaseOrder.setId(20L);
    purchaseOrder.setOrderNo("P2024010100001");

    when(orderMapper.selectById(orderId)).thenReturn(order);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(userMapper.selectById(100L)).thenReturn(user);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(orderMapper.selectById(20L)).thenReturn(purchaseOrder);
    when(packageService.calculateRefundAmount(pkg)).thenReturn(BigDecimal.valueOf(800));

    AdminOrderDetailResponse detail = orderService.detail(1L, orderId);

    assertThat(detail.type()).isEqualTo(OrderType.REFUND.getValue());
    assertThat(detail.calculatedRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
    assertThat(detail.purchaseOrderNo()).isEqualTo("P2024010100001");
  }

  @Test
  @DisplayName("list 按退款状态筛选")
  void list_withRefundStatusFilter_returnsPagedItems() {
    AdminOrderListRequest request = new AdminOrderListRequest(null, "refund_pending", null, 1, 10);
    Page<Order> page = new Page<>(1, 10);
    page.setRecords(Collections.emptyList());
    page.setTotal(0);
    when(orderMapper.selectPage(any(Page.class), any())).thenReturn(page);

    AdminOrderListResponse response = orderService.list(1L, request);

    assertThat(response).isNotNull();
    assertThat(response.total()).isEqualTo(0);
  }

  private Order refundOrder(Long orderId, Long userId, Long packageId) {
    Order order = new Order();
    order.setId(orderId);
    order.setOrderNo("R2024010100001");
    order.setType(OrderType.REFUND.getValue());
    order.setStatus(OrderStatus.REFUND_PENDING.getValue());
    order.setUserId(userId);
    order.setCoachId(1L);
    order.setPackageId(packageId);
    return order;
  }

  private CoursePackage activePackage(
      Long id, BigDecimal paidAmount, int totalHours, int consumedCount, BigDecimal ratio) {
    CoursePackage pkg = new CoursePackage();
    pkg.setId(id);
    pkg.setUserId(100L);
    pkg.setCoachId(1L);
    pkg.setPaidAmount(paidAmount);
    pkg.setTotalHours(totalHours);
    pkg.setConsumedCount(consumedCount);
    pkg.setAvailableCount(totalHours - consumedCount);
    pkg.setRefundEnabled(true);
    pkg.setRefundRatio(ratio);
    pkg.setStatus("active");
    return pkg;
  }

  private CoursePackage frozenPackage(Long id, String frozenReason) {
    CoursePackage pkg = activePackage(id, BigDecimal.valueOf(1000), 10, 0, BigDecimal.ONE);
    pkg.setStatus("frozen");
    pkg.setFrozenReason(frozenReason);
    return pkg;
  }
}
