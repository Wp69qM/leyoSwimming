package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

  @Mock private PackageMapper packageMapper;
  @Mock private OrderMapper orderMapper;
  @Mock private RefundRecordMapper refundRecordMapper;

  @InjectMocks private PackageService packageService;

  @Test
  @DisplayName("requestRefund 创建退款订单并冻结套餐")
  void requestRefund_validPackage_createsOrderAndFreezesPackage() {
    CoursePackage pkg = activePackage(5L, 100L);
    Order purchaseOrder = purchaseOrder(20L, 5L);

    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectOne(any())).thenReturn(purchaseOrder);

    UserPackageRefundResponse response =
        packageService.requestRefund(100L, new UserPackageRefundRequest(5L, "时间冲突"));

    assertThat(response.orderNo()).startsWith("R");
    assertThat(response.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderMapper).insert(orderCaptor.capture());
    Order refundOrder = orderCaptor.getValue();
    assertThat(refundOrder.getType()).isEqualTo(OrderType.REFUND.getValue());
    assertThat(refundOrder.getStatus()).isEqualTo(OrderStatus.REFUND_PENDING.getValue());
    assertThat(refundOrder.getPurchaseOrderId()).isEqualTo(20L);

    ArgumentCaptor<CoursePackage> packageCaptor = ArgumentCaptor.forClass(CoursePackage.class);
    verify(packageMapper).updateById(packageCaptor.capture());
    assertThat(packageCaptor.getValue().getStatus()).isEqualTo("frozen");
    assertThat(packageCaptor.getValue().getFrozenReason()).isEqualTo("refund_pending");
  }

  @Test
  @DisplayName("requestRefund 套餐不属于当前用户时拒绝")
  void requestRefund_packageNotOwned_forbidden() {
    CoursePackage pkg = activePackage(5L, 100L);
    when(packageMapper.selectById(5L)).thenReturn(pkg);

    assertThatThrownBy(
            () -> packageService.requestRefund(999L, new UserPackageRefundRequest(5L, "时间冲突")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
  }

  @Test
  @DisplayName("requestRefund 存在待处理退款时拒绝")
  void requestRefund_pendingRefundExists_rejects() {
    CoursePackage pkg = activePackage(5L, 100L);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(1L);

    assertThatThrownBy(
            () -> packageService.requestRefund(100L, new UserPackageRefundRequest(5L, "时间冲突")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REFUND_PENDING_EXISTS));
  }

  @Test
  @DisplayName("calculateRefundAmount 按剩余课时和退款比例计算")
  void calculateRefundAmount_standardCase_returnsExpected() {
    CoursePackage pkg = activePackage(1L, 100L);
    pkg.setPaidAmount(BigDecimal.valueOf(1000));
    pkg.setTotalHours(10);
    pkg.setConsumedCount(2);
    pkg.setRefundRatio(BigDecimal.valueOf(0.8));

    BigDecimal amount = packageService.calculateRefundAmount(pkg);

    assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(640.00));
  }

  private CoursePackage activePackage(Long id, Long userId) {
    CoursePackage pkg = new CoursePackage();
    pkg.setId(id);
    pkg.setUserId(userId);
    pkg.setCoachId(1L);
    pkg.setTotalHours(10);
    pkg.setConsumedCount(2);
    pkg.setReservedCount(0);
    pkg.setAvailableCount(8);
    pkg.setPaidAmount(BigDecimal.valueOf(1000));
    pkg.setRefundEnabled(true);
    pkg.setRefundRatio(BigDecimal.ONE);
    pkg.setRefundValidDays(30);
    pkg.setStatus("active");
    return pkg;
  }

  private Order purchaseOrder(Long id, Long packageId) {
    Order order = new Order();
    order.setId(id);
    order.setOrderNo("P2024010100001");
    order.setType(OrderType.PURCHASE.getValue());
    order.setStatus(OrderStatus.PAID.getValue());
    order.setPackageId(packageId);
    order.setUserId(100L);
    return order;
  }
}
