package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachApplicationSaveDraftRequest;
import com.leyoswimming.dto.request.CoachApplicationSubmitRequest;
import com.leyoswimming.dto.request.CoachCertificateItem;
import com.leyoswimming.dto.response.CoachApplicationResponse;
import com.leyoswimming.dto.response.CoachApplicationSubmitResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachApplication;
import com.leyoswimming.enums.CoachApplicationStatus;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachApplicationMapper;
import com.leyoswimming.repository.CoachAuditLogMapper;
import com.leyoswimming.repository.CoachCertificateApplicationMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoachOnboardingServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private CoachApplicationMapper applicationMapper;
  @Mock private CoachCertificateApplicationMapper certificateApplicationMapper;
  @Mock private CoachAuditLogMapper auditLogMapper;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private DistributedLockHelper distributedLockHelper;

  private IdCardEncryptor idCardEncryptor;
  private PhoneEncryptor phoneEncryptor;
  private IdempotencyHelper idempotencyHelper;
  private CoachOnboardingService coachOnboardingService;

  @BeforeEach
  void setUp() throws Exception {
    idCardEncryptor = new IdCardEncryptor("local-test-id-card-encryption-key-32b");
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
    idempotencyHelper = new IdempotencyHelper(redisTemplate);
    coachOnboardingService =
        new CoachOnboardingService(
            coachMapper,
            applicationMapper,
            certificateApplicationMapper,
            auditLogMapper,
            idCardEncryptor,
            phoneEncryptor,
            idempotencyHelper,
            distributedLockHelper);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(distributedLockHelper.lock(any(), any(), any(Duration.class)))
        .thenReturn(new LockToken("lock:coach_onboarding:1", "token"));
  }

  @Test
  @DisplayName("获取详情：无申请时返回 coach 基本信息")
  void getDetail_withoutApplication_returnsCoachInfo() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(applicationMapper.findLatestByCoachId(1L)).thenReturn(null);

    CoachApplicationResponse response = coachOnboardingService.getDetail(1L);

    assertThat(response.coachId()).isEqualTo(1L);
    assertThat(response.status()).isEqualTo(CoachStatus.NOT_SUBMITTED.getValue());
    assertThat(response.entryType()).isEqualTo("first");
  }

  @Test
  @DisplayName("保存草稿：首次保存创建 draft 申请")
  void saveDraft_firstTime_createsDraft() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(applicationMapper.findLatestByCoachId(1L)).thenReturn(null);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
    when(applicationMapper.insert(any(CoachApplication.class))).thenAnswer(invocation -> {
      CoachApplication app = invocation.getArgument(0);
      app.setId(100L);
      return 1;
    });

    CoachApplicationSaveDraftRequest request = draftRequest();
    CoachApplicationSubmitResponse response = coachOnboardingService.saveDraft(1L, request);

    assertThat(response.coachId()).isEqualTo(1L);
    assertThat(response.applicationId()).isEqualTo(100L);

    ArgumentCaptor<CoachApplication> appCaptor = ArgumentCaptor.forClass(CoachApplication.class);
    verify(applicationMapper).insert(appCaptor.capture());
    CoachApplication saved = appCaptor.getValue();
    assertThat(saved.getStatus()).isEqualTo(CoachApplicationStatus.DRAFT.getValue());
    assertThat(saved.getName()).isEqualTo("张三");
  }

  @Test
  @DisplayName("保存草稿：存在 pending 申请时抛出 COACH_APPLICATION_PENDING")
  void saveDraft_pendingExists_throwsPending() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    CoachApplication pending = new CoachApplication();
    pending.setStatus(CoachApplicationStatus.PENDING.getValue());
    when(applicationMapper.findLatestByCoachId(1L)).thenReturn(pending);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    assertThatThrownBy(() -> coachOnboardingService.saveDraft(1L, draftRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_APPLICATION_PENDING));
  }

  @Test
  @DisplayName("提交入驻：教练状态不允许时抛出 COACH_STATUS_NOT_ALLOWED")
  void submit_notAllowedStatus_throwsStatusNotAllowed() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    coach.setStatus(CoachStatus.PENDING.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    assertThatThrownBy(() -> coachOnboardingService.submit(1L, submitRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_STATUS_NOT_ALLOWED));
  }

  @Test
  @DisplayName("提交入驻：信息完整时创建 pending 申请并更新教练状态")
  void submit_validRequest_createsPendingApplication() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(applicationMapper.findPendingByCoachId(1L)).thenReturn(null);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);
    when(applicationMapper.insert(any(CoachApplication.class))).thenAnswer(invocation -> {
      CoachApplication app = invocation.getArgument(0);
      app.setId(200L);
      return 1;
    });

    CoachApplicationSubmitResponse response = coachOnboardingService.submit(1L, submitRequest());

    assertThat(response.status()).isEqualTo(CoachStatus.PENDING.getValue());

    ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
    verify(coachMapper).updateById(coachCaptor.capture());
    assertThat(coachCaptor.getValue().getStatus()).isEqualTo(CoachStatus.PENDING.getValue());

    ArgumentCaptor<CoachApplication> appCaptor = ArgumentCaptor.forClass(CoachApplication.class);
    verify(applicationMapper).insert(appCaptor.capture());
    CoachApplication savedApplication = appCaptor.getValue();
    assertThat(savedApplication.getStatus()).isEqualTo(CoachApplicationStatus.PENDING.getValue());
    assertThat(savedApplication.getPhone()).isEqualTo(coach.getPhone());
    assertThat(savedApplication.getTeachingStrokes()).isEqualTo("蛙泳,自由泳");
  }

  @Test
  @DisplayName("提交入驻：缺少证书时抛出 BAD_REQUEST")
  void submit_missingCertificates_throwsBadRequest() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    CoachApplicationSubmitRequest request = new CoachApplicationSubmitRequest(
        "张三", "male", 25, "test@example.com", "http://qr", "110101199001011234",
        5, 100, 1000, List.of("蛙泳"), "简介", BigDecimal.valueOf(200), Collections.emptyList(), "key");

    assertThatThrownBy(() -> coachOnboardingService.submit(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test
  @DisplayName("提交入驻：已存在 pending 申请时抛出 COACH_APPLICATION_PENDING")
  void submit_existingPending_throwsPending() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(applicationMapper.findPendingByCoachId(1L)).thenReturn(new CoachApplication());
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    assertThatThrownBy(() -> coachOnboardingService.submit(1L, submitRequest()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_APPLICATION_PENDING));

    verify(applicationMapper, never()).insert(any(CoachApplication.class));
  }

  @Test
  @DisplayName("提交入驻：身份证格式无效时抛出 BAD_REQUEST")
  void submit_invalidIdCard_throwsBadRequest() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    CoachApplicationSubmitRequest request = new CoachApplicationSubmitRequest(
        "张三", "male", 25, "test@example.com", "http://qr", "11010119900101123Y",
        5, 100, 1000, List.of("蛙泳"), "简介", BigDecimal.valueOf(200),
        List.of(
            new CoachCertificateItem("ID_CARD_FRONT", "http://id-front"),
            new CoachCertificateItem("ID_CARD_BACK", "http://id-back"),
            new CoachCertificateItem("COACH_CERT", "http://coach-cert"),
            new CoachCertificateItem("HEALTH_CERT", "http://health-cert"),
            new CoachCertificateItem("PORTRAIT", "http://portrait")),
        "submit-key");

    assertThatThrownBy(() -> coachOnboardingService.submit(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  @Test
  @DisplayName("提交入驻：证书类型无效时抛出 BAD_REQUEST")
  void submit_invalidCertType_throwsBadRequest() throws Exception {
    Coach coach = notSubmittedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true);

    CoachApplicationSubmitRequest request = new CoachApplicationSubmitRequest(
        "张三", "male", 25, "test@example.com", "http://qr", "110101199001011234",
        5, 100, 1000, List.of("蛙泳"), "简介", BigDecimal.valueOf(200),
        List.of(new CoachCertificateItem("INVALID_CERT", "http://img")),
        "submit-key");

    assertThatThrownBy(() -> coachOnboardingService.submit(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST));
  }

  private Coach notSubmittedCoach(Long id) throws Exception {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setOpenid("openid_" + id);
    coach.setPhone(phoneEncryptor.encrypt("13800138000"));
    coach.setPhoneHash(phoneEncryptor.hash("13800138000"));
    coach.setStatus(CoachStatus.NOT_SUBMITTED.getValue());
    coach.setName("原姓名");
    return coach;
  }

  private CoachApplicationSaveDraftRequest draftRequest() {
    return new CoachApplicationSaveDraftRequest(
        null, "张三", "male", 25, "test@example.com", "http://qr",
        "110101199001011234", 5, 100, 1000, List.of("蛙泳"), "个人简介示例",
        BigDecimal.valueOf(200), List.of(new CoachCertificateItem("PORTRAIT", "http://img")),
        "draft-key");
  }

  private CoachApplicationSubmitRequest submitRequest() {
    return new CoachApplicationSubmitRequest(
        "张三", "male", 25, "test@example.com", "http://qr", "110101199001011234",
        5, 100, 1000, List.of("蛙泳", "自由泳"), "个人简介示例", BigDecimal.valueOf(200),
        List.of(
            new CoachCertificateItem("ID_CARD_FRONT", "http://id-front"),
            new CoachCertificateItem("ID_CARD_BACK", "http://id-back"),
            new CoachCertificateItem("COACH_CERT", "http://coach-cert"),
            new CoachCertificateItem("HEALTH_CERT", "http://health-cert"),
            new CoachCertificateItem("PORTRAIT", "http://portrait")),
        "submit-key");
  }
}
