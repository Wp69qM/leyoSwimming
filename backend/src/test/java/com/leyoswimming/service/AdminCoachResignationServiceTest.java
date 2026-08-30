package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.AdminResignationTicketDetailResponse;
import com.leyoswimming.dto.response.AdminResignationTicketListResponse;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.entity.Booking;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachResignationAction;
import com.leyoswimming.entity.CoachResignationTicket;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.ScheduleSlot;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.repository.BookingMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachResignationActionMapper;
import com.leyoswimming.repository.CoachResignationTicketMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.ScheduleSlotMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class AdminCoachResignationServiceTest {

  @Mock private CoachResignationTicketMapper ticketMapper;
  @Mock private CoachResignationActionMapper actionMapper;
  @Mock private CoachMapper coachMapper;
  @Mock private PackageMapper packageMapper;
  @Mock private RefundRecordMapper refundRecordMapper;
  @Mock private BookingMapper bookingMapper;
  @Mock private ScheduleSlotMapper scheduleSlotMapper;
  @Mock private UserMapper userMapper;
  @Mock private AdminUserMapper adminUserMapper;
  private PhoneEncryptor phoneEncryptor;

  private AdminCoachResignationService adminCoachResignationService;

  @BeforeEach
  void setUp() throws Exception {
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    adminCoachResignationService =
        new AdminCoachResignationService(
            ticketMapper,
            actionMapper,
            coachMapper,
            packageMapper,
            refundRecordMapper,
            bookingMapper,
            scheduleSlotMapper,
            userMapper,
            adminUserMapper,
            phoneEncryptor);
  }

  @Test
  @DisplayName("工单列表：按状态过滤返回分页结果")
  void list_withStatusFilter_returnsPagedTickets() {
    CoachResignationTicket ticket = pendingTicket(100L, 1L);
    Coach coach = coach(1L, "Coach", null);
    Page<CoachResignationTicket> page = new Page<>(1, 20);
    page.setRecords(List.of(ticket));
    page.setTotal(1);

    when(ticketMapper.selectPage(any(Page.class), any())).thenReturn(page);
    when(coachMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(coach));

    AdminResignationTicketListResponse response =
        adminCoachResignationService.list("pending_audit", 1, 20);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).ticketId()).isEqualTo(100L);
    assertThat(response.total()).isEqualTo(1);
  }

  @Test
  @DisplayName("工单列表：无过滤返回全部")
  void list_withoutStatusFilter_returnsAllTickets() {
    CoachResignationTicket ticket = pendingTicket(100L, 1L);
    Page<CoachResignationTicket> page = new Page<>(1, 20);
    page.setRecords(List.of(ticket));
    page.setTotal(1);

    when(ticketMapper.selectPage(any(Page.class), any())).thenReturn(page);
    when(coachMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(coach(1L, "Coach", null)));

    AdminResignationTicketListResponse response =
        adminCoachResignationService.list(null, 1, 20);

    assertThat(response.items()).hasSize(1);
  }

  @Test
  @DisplayName("工单详情：存在工单返回详情")
  void detail_existingTicket_returnsDetail() {
    CoachResignationTicket ticket = pendingTicket(100L, 1L);
    ticket.setScheduleCleared(true);
    ticket.setSettlementStatus(1);

    CoursePackage pkg = activePackage(10L, 1L, 200L);
    CoachResignationAction action = refundAction(100L, 10L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(coachMapper.selectById(1L)).thenReturn(coach(1L, "Coach", null));
    when(packageMapper.findActiveByCoachId(1L)).thenReturn(List.of(pkg));
    when(actionMapper.findByTicketId(100L)).thenReturn(List.of(action));

    AdminResignationTicketDetailResponse response = adminCoachResignationService.detail(100L);

    assertThat(response.ticketId()).isEqualTo(100L);
    assertThat(response.checklist().allActionsRegistered()).isTrue();
    assertThat(response.checklist().scheduleCleared()).isTrue();
    assertThat(response.checklist().settlementCompleted()).isTrue();
    assertThat(response.packages()).hasSize(1);
    assertThat(response.packages().get(0).action()).isEqualTo("refund");
  }

  @Test
  @DisplayName("工单详情：不存在工单抛出 RESOURCE_NOT_FOUND")
  void detail_nonExistingTicket_throwsResourceNotFound() {
    when(ticketMapper.selectById(100L)).thenReturn(null);

    assertThatThrownBy(() -> adminCoachResignationService.detail(100L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  @DisplayName("审批通过：所有检查项完成，更新套餐、预约、排班和教练状态")
  void approve_allChecksPassed_approvesTicket() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "SUPER_ADMIN"));

    CoachResignationTicket ticket = pendingTicket(100L, 2L);
    ticket.setScheduleCleared(true);

    CoursePackage pkg = activePackage(10L, 2L, 200L);
    CoachResignationAction action = refundAction(100L, 10L);
    Coach coach = coach(2L, "Coach", null);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.findActiveByCoachId(2L)).thenReturn(List.of(pkg));
    when(actionMapper.findByTicketId(100L)).thenReturn(List.of(action));
    when(refundRecordMapper.selectOne(any())).thenReturn(null);
    when(bookingMapper.findFutureActiveByCoachId(eq(2L), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    when(coachMapper.selectById(2L)).thenReturn(coach);

    adminCoachResignationService.approve(1L, 100L, null);

    ArgumentCaptor<CoursePackage> packageCaptor = ArgumentCaptor.forClass(CoursePackage.class);
    verify(packageMapper).updateById(packageCaptor.capture());
    assertThat(packageCaptor.getValue().getStatus()).isEqualTo("frozen");
    assertThat(packageCaptor.getValue().getFrozenReason()).isEqualTo("coach_resigned");

    ArgumentCaptor<CoachResignationAction> actionCaptor =
        ArgumentCaptor.forClass(CoachResignationAction.class);
    verify(actionMapper).updateById(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getStatus()).isEqualTo("approved");

    ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
    verify(coachMapper).updateById(coachCaptor.capture());
    assertThat(coachCaptor.getValue().getStatus()).isEqualTo(CoachStatus.RESIGNED.getValue());

    ArgumentCaptor<CoachResignationTicket> ticketCaptor =
        ArgumentCaptor.forClass(CoachResignationTicket.class);
    verify(ticketMapper).updateById(ticketCaptor.capture());
    assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("approved");
  }

  @Test
  @DisplayName("审批通过：套餐未全部登记抛出 CHECKLIST_NOT_PASSED")
  void approve_missingActions_throwsChecklistNotPassed() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "SUPER_ADMIN"));

    CoachResignationTicket ticket = pendingTicket(100L, 2L);
    ticket.setScheduleCleared(true);

    CoursePackage pkg = activePackage(10L, 2L, 200L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.findActiveByCoachId(2L)).thenReturn(List.of(pkg));
    when(actionMapper.findByTicketId(100L)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> adminCoachResignationService.approve(1L, 100L, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CHECKLIST_NOT_PASSED));

    verify(packageMapper, never()).updateById(any(CoursePackage.class));
  }

  @Test
  @DisplayName("审批通过：未来排班未清空抛出 SCHEDULE_NOT_CLEARED")
  void approve_scheduleNotCleared_throwsScheduleNotCleared() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "SUPER_ADMIN"));

    CoachResignationTicket ticket = pendingTicket(100L, 2L);
    CoursePackage pkg = activePackage(10L, 2L, 200L);
    CoachResignationAction action = refundAction(100L, 10L);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(packageMapper.findActiveByCoachId(2L)).thenReturn(List.of(pkg));
    when(actionMapper.findByTicketId(100L)).thenReturn(List.of(action));
    when(scheduleSlotMapper.findFirstVisibleFutureByCoachId(any(), any()))
        .thenReturn(new ScheduleSlot());

    assertThatThrownBy(() -> adminCoachResignationService.approve(1L, 100L, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SCHEDULE_NOT_CLEARED));
  }

  @Test
  @DisplayName("审批通过：工单非待审批状态抛出 TICKET_NOT_PENDING_AUDIT")
  void approve_nonPendingTicket_throwsTicketNotPendingAudit() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "SUPER_ADMIN"));

    CoachResignationTicket ticket = pendingTicket(100L, 2L);
    ticket.setStatus("processing");

    when(ticketMapper.selectById(100L)).thenReturn(ticket);

    assertThatThrownBy(() -> adminCoachResignationService.approve(1L, 100L, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TICKET_NOT_PENDING_AUDIT));
  }

  @Test
  @DisplayName("审批通过：无权限管理员抛出 FORBIDDEN")
  void approve_unauthorizedAdmin_throwsForbidden() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "FINANCE"));

    assertThatThrownBy(() -> adminCoachResignationService.approve(1L, 100L, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN));

    verify(ticketMapper, never()).selectById(any());
  }

  @Test
  @DisplayName("审批拒绝：恢复教练状态并更新工单")
  void reject_validTicket_rejectsAndRestoresCoach() {
    when(adminUserMapper.selectById(1L)).thenReturn(adminUser(1L, "COACH_MANAGER"));

    CoachResignationTicket ticket = pendingTicket(100L, 2L);
    Coach coach = coach(2L, "Coach", null);

    when(ticketMapper.selectById(100L)).thenReturn(ticket);
    when(coachMapper.selectById(2L)).thenReturn(coach);

    adminCoachResignationService.reject(1L, 100L, "资料不全");

    ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
    verify(coachMapper).updateById(coachCaptor.capture());
    assertThat(coachCaptor.getValue().getStatus()).isEqualTo(CoachStatus.APPROVED.getValue());

    ArgumentCaptor<CoachResignationTicket> ticketCaptor =
        ArgumentCaptor.forClass(CoachResignationTicket.class);
    verify(ticketMapper).updateById(ticketCaptor.capture());
    assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("rejected");
    assertThat(ticketCaptor.getValue().getReason()).contains("资料不全");
  }

  private AdminUser adminUser(Long id, String role) {
    AdminUser admin = new AdminUser();
    admin.setId(id);
    admin.setUsername("admin" + id);
    admin.setRole(role);
    admin.setStatus(0);
    return admin;
  }

  private Coach coach(Long id, String name, String phone) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setName(name);
    coach.setPhone(phone);
    coach.setStatus(CoachStatus.RESIGNING.getValue());
    return coach;
  }

  private CoachResignationTicket pendingTicket(Long id, Long coachId) {
    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setId(id);
    ticket.setCoachId(coachId);
    ticket.setTicketNo("CR" + id);
    ticket.setStatus("pending_audit");
    ticket.setTotalPackages(1);
    ticket.setHandledPackages(1);
    ticket.setScheduleCleared(false);
    ticket.setSettlementStatus(0);
    return ticket;
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

  private CoachResignationAction refundAction(Long ticketId, Long packageId) {
    CoachResignationAction action = new CoachResignationAction();
    action.setId(500L);
    action.setTicketId(ticketId);
    action.setPackageId(packageId);
    action.setAction("refund");
    action.setStatus("registered");
    return action;
  }
}
