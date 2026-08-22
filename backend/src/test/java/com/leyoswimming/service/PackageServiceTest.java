package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPackageDetailRequest;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserActivePackageResponse;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
  @Mock private DistributedLockHelper lockHelper;

  @InjectMocks private PackageService packageService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(lockHelper.lock(any(), any(), any()))
        .thenReturn(new LockToken("key", "token"));
  }

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

  @Test
  @DisplayName("getActivePackage 返回用户当前 active 套餐")
  void getActivePackage_hasActivePackage_returnsPackage() {
    CoursePackage pkg = activePackage(7L, 100L);
    pkg.setCoachName("教练 A");

    when(packageMapper.findFirstActiveByUserId(100L)).thenReturn(pkg);

    UserActivePackageResponse response = packageService.getActivePackage(100L);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.coachId()).isEqualTo(1L);
    assertThat(response.coachName()).isEqualTo("教练 A");
    assertThat(response.status()).isEqualTo("active");
  }

  @Test
  @DisplayName("getActivePackage 无 active 套餐时返回 null")
  void getActivePackage_noActivePackage_returnsNull() {
    when(packageMapper.findFirstActiveByUserId(100L)).thenReturn(null);

    UserActivePackageResponse response = packageService.getActivePackage(100L);

    assertThat(response).isNull();
  }

  @Test
  @DisplayName("detail 返回套餐详情")
  void detail_existingPackageOwnedByUser_returnsDetail() {
    CoursePackage pkg = activePackage(5L, 100L);
    pkg.setPackageName("10 节正价课");
    pkg.setCoachName("教练 A");
    pkg.setPackageMode("standard");
    pkg.setTeachingType("一对一");
    pkg.setStrokeIds(List.of(1, 2));
    pkg.setDurationMinutes(60);
    pkg.setValidDays(90);
    pkg.setExpireAt(LocalDateTime.of(2026, 10, 30, 23, 59, 59));
    pkg.setCreatedAt(LocalDateTime.of(2026, 8, 2, 14, 30, 0));

    when(packageMapper.selectById(5L)).thenReturn(pkg);

    var response = packageService.detail(100L, new UserPackageDetailRequest(5L));

    assertThat(response.packageId()).isEqualTo(5L);
    assertThat(response.packageName()).isEqualTo("10 节正价课");
    assertThat(response.coachName()).isEqualTo("教练 A");
    assertThat(response.canRefund()).isTrue();
  }

  @Test
  @DisplayName("detail 套餐不存在时抛出错误")
  void detail_packageNotFound_throws() {
    when(packageMapper.selectById(5L)).thenReturn(null);

    assertThatThrownBy(() -> packageService.detail(100L, new UserPackageDetailRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PACKAGE_NOT_FOUND));
  }

  @Test
  @DisplayName("detail 套餐不属于当前用户时拒绝")
  void detail_packageNotOwned_forbidden() {
    CoursePackage pkg = activePackage(5L, 100L);
    when(packageMapper.selectById(5L)).thenReturn(pkg);

    assertThatThrownBy(() -> packageService.detail(999L, new UserPackageDetailRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
  }

  @Test
  @DisplayName("requestRefund 允许 expired 状态套餐申请退款")
  void requestRefund_expiredPackage_createsOrder() {
    CoursePackage pkg = activePackage(5L, 100L);
    pkg.setStatus("expired");
    Order purchaseOrder = purchaseOrder(20L, 5L);

    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectOne(any())).thenReturn(purchaseOrder);

    UserPackageRefundResponse response =
        packageService.requestRefund(100L, new UserPackageRefundRequest(5L, "时间冲突"));

    assertThat(response.orderNo()).startsWith("R");
    assertThat(response.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
  }

  @Test
  @DisplayName("requestRefund 允许 frozen(coach_resigned) 状态套餐申请全额退款")
  void requestRefund_coachResignedPackage_createsFullRefundOrder() {
    CoursePackage pkg = activePackage(5L, 100L);
    pkg.setStatus("frozen");
    pkg.setFrozenReason("coach_resigned");
    pkg.setConsumedCount(5);
    pkg.setAvailableCount(5);
    Order purchaseOrder = purchaseOrder(20L, 5L);

    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(0L);
    when(orderMapper.selectOne(any())).thenReturn(purchaseOrder);

    UserPackageRefundResponse response =
        packageService.requestRefund(100L, new UserPackageRefundRequest(5L, "教练离职"));

    assertThat(response.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
  }

  @Test
  @DisplayName("calculateRefundAmount 教练离职时返回全额")
  void calculateRefundAmount_coachResigned_returnsFullPaidAmount() {
    CoursePackage pkg = activePackage(1L, 100L);
    pkg.setPaidAmount(BigDecimal.valueOf(1000));
    pkg.setStatus("frozen");
    pkg.setFrozenReason("coach_resigned");
    pkg.setTotalHours(10);
    pkg.setConsumedCount(8);

    BigDecimal amount = packageService.calculateRefundAmount(pkg);

    assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
  }

  @Test
  @DisplayName("canRefund 排除存在待处理退款订单的套餐")
  void detail_packageWithPendingRefundOrder_canRefundFalse() {
    CoursePackage pkg = activePackage(5L, 100L);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(1L);

    var response = packageService.detail(100L, new UserPackageDetailRequest(5L));

    assertThat(response.canRefund()).isFalse();
  }

  @Test
  @DisplayName("buildRefundRuleText 教练离职显示全额退款")
  void detail_coachResignedPackage_refundRuleTextFullRefund() {
    CoursePackage pkg = activePackage(5L, 100L);
    pkg.setStatus("frozen");
    pkg.setFrozenReason("coach_resigned");
    pkg.setRefundEnabled(true);
    when(packageMapper.selectById(5L)).thenReturn(pkg);
    when(orderMapper.selectCount(any())).thenReturn(0L);

    var response = packageService.detail(100L, new UserPackageDetailRequest(5L));

    assertThat(response.refundRuleText()).isEqualTo("教练离职，可申请 100% 全额退款");
  }

  @Test
  @DisplayName("buildRefundRuleText 不支持退款时显示不支持退款")
  void detail_refundDisabled_refundRuleTextNotSupported() {
    CoursePackage pkg = activePackage(5L, 100L);
    pkg.setRefundEnabled(false);
    when(packageMapper.selectById(5L)).thenReturn(pkg);

    var response = packageService.detail(100L, new UserPackageDetailRequest(5L));

    assertThat(response.refundRuleText()).isEqualTo("不支持退款");
    assertThat(response.canRefund()).isFalse();
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
