package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminCoachApplicationApproveRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationListRequest;
import com.leyoswimming.dto.request.AdminCoachApplicationRejectRequest;
import com.leyoswimming.dto.response.AdminCoachApplicationDetailResponse;
import com.leyoswimming.dto.response.AdminCoachApplicationListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachApplication;
import com.leyoswimming.entity.CoachAuditLog;
import com.leyoswimming.enums.CoachApplicationStatus;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.repository.CoachApplicationMapper;
import com.leyoswimming.repository.CoachAuditLogMapper;
import com.leyoswimming.repository.CoachCertificateApplicationMapper;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.PhoneEncryptor;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoachAuditServiceTest {

  @Mock private CoachApplicationMapper applicationMapper;
  @Mock private CoachCertificateApplicationMapper certificateApplicationMapper;
  @Mock private CoachCertificateMapper certificateMapper;
  @Mock private CoachAuditLogMapper auditLogMapper;
  @Mock private CoachMapper coachMapper;
  @Mock private DistributedLockHelper distributedLockHelper;
  @Mock private AdminUserMapper adminUserMapper;

  private IdCardEncryptor idCardEncryptor;
  private PhoneEncryptor phoneEncryptor;
  private CoachAuditService coachAuditService;

  @BeforeEach
  void setUp() throws Exception {
    idCardEncryptor = new IdCardEncryptor("local-test-id-card-encryption-key-32b");
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    coachAuditService =
        new CoachAuditService(
            applicationMapper,
            certificateApplicationMapper,
            certificateMapper,
            auditLogMapper,
            coachMapper,
            phoneEncryptor,
            idCardEncryptor,
            distributedLockHelper,
            adminUserMapper);
    when(distributedLockHelper.lock(any(), any(), any(Duration.class)))
        .thenReturn(new LockToken("lock:coach_audit:100", "token"));
  }

  @Test
  @DisplayName("列表查询：返回分页结果")
  void list_withStatus_returnsPagedResult() {
    CoachApplication app = pendingApplication(1L, 100L);
    Page<CoachApplication> page = new Page<>(1, 10);
    page.setRecords(List.of(app));
    page.setTotal(1);
    when(applicationMapper.selectPage(any(), any())).thenReturn(page);

    AdminCoachApplicationListResponse response =
        coachAuditService.list(new AdminCoachApplicationListRequest("pending", null, 1, 10));

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.list()).hasSize(1);
    assertThat(response.list().get(0).coachId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("详情查询：返回申请详情")
  void detail_existingApplication_returnsDetail() throws Exception {
    CoachApplication app = pendingApplication(1L, 100L);
    app.setPhone(phoneEncryptor.encrypt("13800138000"));
    when(applicationMapper.selectById(100L)).thenReturn(app);
    when(certificateApplicationMapper.findByApplicationId(100L)).thenReturn(Collections.emptyList());
    when(applicationMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(auditLogMapper.selectList(any())).thenReturn(Collections.emptyList());

    AdminCoachApplicationDetailResponse response = coachAuditService.detail(100L);

    assertThat(response.coachId()).isEqualTo(1L);
    assertThat(response.phone()).isEqualTo("138****8000");
  }

  @Test
  @DisplayName("详情查询：申请不存在抛出 RESOURCE_NOT_FOUND")
  void detail_notFound_throwsResourceNotFound() {
    when(applicationMapper.selectById(100L)).thenReturn(null);

    assertThatThrownBy(() -> coachAuditService.detail(100L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  @DisplayName("通过审核：pending 申请更新为 approved 并复制数据到 coach")
  void approve_pendingApplication_updatesCoachAndApplication() throws Exception {
    CoachApplication app = pendingApplication(1L, 100L);
    app.setPhone(phoneEncryptor.encrypt("13800138000"));
    app.setIdCardNo(idCardEncryptor.encrypt("110101199001011234"));
    Coach coach = coach(1L, CoachStatus.PENDING.getValue());
    when(adminUserMapper.selectById(10L)).thenReturn(adminUser("SUPER_ADMIN"));
    when(applicationMapper.selectById(100L)).thenReturn(app);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(certificateApplicationMapper.findByApplicationId(100L)).thenReturn(Collections.emptyList());

    coachAuditService.approve(10L, new AdminCoachApplicationApproveRequest(100L, "通过"));

    assertThat(coach.getStatus()).isEqualTo(CoachStatus.APPROVED.getValue());
    assertThat(coach.getApprovedAt()).isNotNull();
    assertThat(app.getStatus()).isEqualTo(CoachApplicationStatus.APPROVED.getValue());
    assertThat(app.getApprovedBy()).isEqualTo(10L);

    ArgumentCaptor<CoachAuditLog> logCaptor = ArgumentCaptor.forClass(CoachAuditLog.class);
    verify(auditLogMapper).insert(logCaptor.capture());
    assertThat(logCaptor.getValue().getAction()).isEqualTo("approve");
  }

  @Test
  @DisplayName("通过审核：非 pending 状态抛出 BAD_REQUEST")
  void approve_notPending_throwsBadRequest() {
    CoachApplication app = pendingApplication(1L, 100L);
    app.setStatus(CoachApplicationStatus.APPROVED.getValue());
    when(adminUserMapper.selectById(10L)).thenReturn(adminUser("SUPER_ADMIN"));
    when(applicationMapper.selectById(100L)).thenReturn(app);

    assertThatThrownBy(
            () ->
                coachAuditService.approve(
                    10L, new AdminCoachApplicationApproveRequest(100L, "通过")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test
  @DisplayName("驳回审核：首次提交拒绝后教练状态为 rejected")
  void reject_firstSubmission_updatesToRejected() throws Exception {
    CoachApplication app = pendingApplication(1L, 100L);
    app.setPreviousCoachStatus(CoachStatus.NOT_SUBMITTED.getValue());
    Coach coach = coach(1L, CoachStatus.PENDING.getValue());
    when(adminUserMapper.selectById(10L)).thenReturn(adminUser("COACH_MANAGER"));
    when(applicationMapper.selectById(100L)).thenReturn(app);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    coachAuditService.reject(
        10L, new AdminCoachApplicationRejectRequest(100L, "资料不清晰"));

    assertThat(coach.getStatus()).isEqualTo(CoachStatus.REJECTED.getValue());
    assertThat(app.getStatus()).isEqualTo(CoachApplicationStatus.REJECTED.getValue());
    assertThat(app.getRejectionReason()).isEqualTo("资料不清晰");

    ArgumentCaptor<CoachAuditLog> logCaptor = ArgumentCaptor.forClass(CoachAuditLog.class);
    verify(auditLogMapper).insert(logCaptor.capture());
    assertThat(logCaptor.getValue().getAction()).isEqualTo("reject");
  }

  @Test
  @DisplayName("驳回审核：重新入驻被拒绝保持 resigned 状态")
  void reject_reapplyFromResigned_keepsResigned() throws Exception {
    CoachApplication app = pendingApplication(1L, 100L);
    app.setPreviousCoachStatus(CoachStatus.RESIGNED.getValue());
    Coach coach = coach(1L, CoachStatus.PENDING.getValue());
    when(adminUserMapper.selectById(10L)).thenReturn(adminUser("COACH_MANAGER"));
    when(applicationMapper.selectById(100L)).thenReturn(app);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    coachAuditService.reject(
        10L, new AdminCoachApplicationRejectRequest(100L, "不符合条件"));

    assertThat(coach.getStatus()).isEqualTo(CoachStatus.RESIGNED.getValue());
  }

  private Coach coach(Long id, int status) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setOpenid("openid_" + id);
    coach.setStatus(status);
    return coach;
  }

  private AdminUser adminUser(String role) {
    AdminUser admin = new AdminUser();
    admin.setId(10L);
    admin.setRole(role);
    return admin;
  }

  private CoachApplication pendingApplication(Long coachId, Long applicationId) {
    CoachApplication app = new CoachApplication();
    app.setId(applicationId);
    app.setCoachId(coachId);
    app.setStatus(CoachApplicationStatus.PENDING.getValue());
    app.setName("张三");
    app.setAge(25);
    app.setPreviousCoachStatus(CoachStatus.NOT_SUBMITTED.getValue());
    return app;
  }
}
