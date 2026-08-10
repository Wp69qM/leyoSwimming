package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.CoachResignationApplyResponse;
import com.leyoswimming.dto.response.CoachResignationDetailResponse;
import com.leyoswimming.dto.response.CoachResignationPackageActionResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachResignationAction;
import com.leyoswimming.entity.CoachResignationTicket;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachResignationActionMapper;
import com.leyoswimming.repository.CoachResignationTicketMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachResignationServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private PackageMapper packageMapper;
  @Mock private CoachResignationTicketMapper ticketMapper;
  @Mock private CoachResignationActionMapper actionMapper;
  @Mock private RefundRecordMapper refundRecordMapper;
  @Mock private UserMapper userMapper;
  private PhoneEncryptor phoneEncryptor;

  private CoachResignationService coachResignationService;

  @BeforeEach
  void setUp() throws Exception {
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    coachResignationService =
        new CoachResignationService(
            coachMapper,
            packageMapper,
            ticketMapper,
            actionMapper,
            refundRecordMapper,
            userMapper,
            phoneEncryptor);
  }

  @Test
  @DisplayName("申请离职：已认证教练无活跃套餐，创建工单并更新状态为离职中")
  void apply_approvedCoachWithoutPackages_createsTicket() {
    when(coachMapper.selectById(1L)).thenReturn(approvedCoach(1L));
    when(ticketMapper.findActiveByCoachId(1L)).thenReturn(null);
    when(packageMapper.findActiveByCoachId(1L)).thenReturn(Collections.emptyList());
    when(ticketMapper.insert(any(CoachResignationTicket.class))).thenAnswer(invocation -> {
      CoachResignationTicket t = invocation.getArgument(0);
      t.setId(100L);
      return 1;
    });

    CoachResignationApplyResponse response = coachResignationService.apply(1L, "个人原因", "key");

    assertThat(response.ticketId()).isEqualTo(100L);
    assertThat(response.totalPackages()).isZero();
    assertThat(response.message()).isEqualTo("离职申请已提交，请处理学员套餐");

    ArgumentCaptor<CoachResignationTicket> ticketCaptor =
        ArgumentCaptor.forClass(CoachResignationTicket.class);
    verify(ticketMapper).insert(ticketCaptor.capture());
    CoachResignationTicket ticket = ticketCaptor.getValue();
    assertThat(ticket.getCoachId()).isEqualTo(1L);
    assertThat(ticket.getStatus()).isEqualTo("processing");
    assertThat(ticket.getTotalPackages()).isZero();

    ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
    verify(coachMapper).updateById(coachCaptor.capture());
    assertThat(coachCaptor.getValue().getStatus()).isEqualTo(CoachStatus.RESIGNING.getValue());
  }

  @Test
  @DisplayName("申请离职：教练不存在抛出 COACH_NOT_FOUND")
  void apply_coachNotFound_throwsCoachNotFound() {
    when(coachMapper.selectById(1L)).thenReturn(null);

    assertThatThrownBy(() -> coachResignationService.apply(1L, "reason", "key"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("申请离职：非已认证教练抛出 COACH_STATUS_NOT_ALLOWED")
  void apply_notApprovedCoach_throwsCoachStatusNotAllowed() {
    Coach coach = approvedCoach(1L);
    coach.setStatus(CoachStatus.PENDING.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(() -> coachResignationService.apply(1L, "reason", "key"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_STATUS_NOT_ALLOWED));
  }

  @Test
  @DisplayName("申请离职：已存在进行中的工单抛出 RESIGNATION_ALREADY_PENDING")
  void apply_existingPendingTicket_throwsResignationAlreadyPending() {
    when(coachMapper.selectById(1L)).thenReturn(approvedCoach(1L));
    when(ticketMapper.findActiveByCoachId(1L)).thenReturn(new CoachResignationTicket());

    assertThatThrownBy(() -> coachResignationService.apply(1L, "reason", "key"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESIGNATION_ALREADY_PENDING));

    verify(ticketMapper, never()).insert(any(CoachResignationTicket.class));
  }

  @Test
  @DisplayName("查询离职详情：存在进行中的工单返回详情")
  void detail_withProcessingTicket_returnsDetail() {
    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setId(100L);
    ticket.setCoachId(1L);
    ticket.setStatus("processing");
    ticket.setTotalPackages(2);
    ticket.setHandledPackages(0);

    CoursePackage pkg1 = activePackage(10L, 1L, 100L);
    CoursePackage pkg2 = activePackage(11L, 1L, 101L);
    User user = new User();
    user.setId(100L);
    user.setName("User");

    when(ticketMapper.findActiveByCoachId(1L)).thenReturn(ticket);
    when(packageMapper.findActiveByCoachId(1L)).thenReturn(List.of(pkg1, pkg2));
    when(actionMapper.findByTicketId(100L)).thenReturn(Collections.emptyList());
    when(userMapper.selectById(100L)).thenReturn(user);

    CoachResignationDetailResponse response = coachResignationService.detail(1L);

    assertThat(response).isNotNull();
    assertThat(response.ticketId()).isEqualTo(100L);
    assertThat(response.packages()).hasSize(2);
    assertThat(response.packages().get(0).userName()).isEqualTo("User");
  }

  @Test
  @DisplayName("查询离职详情：无工单返回 null")
  void detail_withoutTicket_returnsNull() {
    when(ticketMapper.findActiveByCoachId(1L)).thenReturn(null);

    assertThat(coachResignationService.detail(1L)).isNull();
  }

  @Test
  @DisplayName("登记套餐处理：选择退款创建 action 和退款记录")
  void registerAction_refund_createsActionAndRefundRecord() {
    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setId(100L);
    ticket.setCoachId(1L);
    ticket.setStatus("processing");

    CoursePackage pkg = activePackage(10L, 1L, 100L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.selectById(10L)).thenReturn(pkg);
    when(actionMapper.findByTicketIdAndPackageId(100L, 10L)).thenReturn(null);
    when(actionMapper.insert(any(CoachResignationAction.class))).thenAnswer(invocation -> {
      CoachResignationAction a = invocation.getArgument(0);
      a.setId(200L);
      return 1;
    });
    when(actionMapper.findByTicketId(100L)).thenReturn(Collections.emptyList());
    when(refundRecordMapper.selectOne(any())).thenReturn(null);

    CoachResignationPackageActionResponse response =
        coachResignationService.registerAction(1L, 100L, 10L, "refund", null);

    assertThat(response.actionId()).isEqualTo(200L);
    assertThat(response.action()).isEqualTo("refund");
    assertThat(response.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));

    ArgumentCaptor<CoachResignationAction> actionCaptor =
        ArgumentCaptor.forClass(CoachResignationAction.class);
    verify(actionMapper).insert(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getAction()).isEqualTo("refund");

    ArgumentCaptor<com.leyoswimming.entity.RefundRecord> refundCaptor =
        ArgumentCaptor.forClass(com.leyoswimming.entity.RefundRecord.class);
    verify(refundRecordMapper).insert(refundCaptor.capture());
    assertThat(refundCaptor.getValue().getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
  }

  @Test
  @DisplayName("登记套餐处理：无效操作抛出 BAD_REQUEST")
  void registerAction_invalidAction_throwsBadRequest() {
    assertThatThrownBy(
            () -> coachResignationService.registerAction(1L, 100L, 10L, "unknown", null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test
  @DisplayName("登记套餐处理：非当前教练套餐抛出 NOT_OWN_PACKAGE")
  void registerAction_notOwnPackage_throwsNotOwnPackage() {
    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setId(100L);
    ticket.setCoachId(1L);
    ticket.setStatus("processing");

    CoursePackage pkg = activePackage(10L, 2L, 100L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.selectById(10L)).thenReturn(pkg);

    assertThatThrownBy(
            () -> coachResignationService.registerAction(1L, 100L, 10L, "refund", null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_OWN_PACKAGE));
  }

  @Test
  @DisplayName("提交审批：自动为未登记套餐创建退款 action")
  void submit_withMissingActions_autoCreatesRefundActions() {
    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setId(100L);
    ticket.setCoachId(1L);
    ticket.setStatus("processing");
    ticket.setHandledPackages(0);

    CoursePackage pkg = activePackage(10L, 1L, 100L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.findActiveByCoachId(1L)).thenReturn(List.of(pkg));
    when(actionMapper.findByTicketId(100L)).thenReturn(Collections.emptyList());
    when(refundRecordMapper.selectOne(any())).thenReturn(null);

    coachResignationService.submit(1L, 100L);

    ArgumentCaptor<CoachResignationTicket> ticketCaptor =
        ArgumentCaptor.forClass(CoachResignationTicket.class);
    verify(ticketMapper).updateById(ticketCaptor.capture());
    CoachResignationTicket updated = ticketCaptor.getValue();
    assertThat(updated.getStatus()).isEqualTo("pending_audit");
    assertThat(updated.getSubmittedAt()).isNotNull();
    assertThat(updated.getHandledPackages()).isEqualTo(1);

    verify(actionMapper).insert(any(CoachResignationAction.class));
    verify(refundRecordMapper).insert(any(com.leyoswimming.entity.RefundRecord.class));
  }

  private Coach approvedCoach(Long id) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setOpenid("openid_" + id);
    coach.setPhone("phone_" + id);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    return coach;
  }

  private CoursePackage activePackage(Long packageId, Long coachId, Long userId) {
    CoursePackage pkg = new CoursePackage();
    pkg.setId(packageId);
    pkg.setUserId(userId);
    pkg.setCoachId(coachId);
    pkg.setTotalHours(10);
    pkg.setConsumedCount(0);
    pkg.setReservedCount(0);
    pkg.setAvailableCount(10);
    pkg.setPricePerHour(BigDecimal.valueOf(200));
    pkg.setPaidAmount(BigDecimal.valueOf(2000));
    pkg.setStatus("active");
    return pkg;
  }
}
