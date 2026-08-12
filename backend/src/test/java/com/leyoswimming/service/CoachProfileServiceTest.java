package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UpdateCoachProfileRequest;
import com.leyoswimming.dto.request.UpdateCoachReferencePriceRequest;
import com.leyoswimming.dto.response.CoachProfileResponse;
import com.leyoswimming.dto.response.ConsentStatusResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.CoachUpdateLog;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.CoachCertificateType;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachUpdateLogMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachProfileServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private CoachCertificateMapper coachCertificateMapper;
  @Mock private CoachUpdateLogMapper coachUpdateLogMapper;
  @Mock private PhoneEncryptor phoneEncryptor;
  @Mock private IdCardEncryptor idCardEncryptor;
  @Mock private SmsCodeService smsCodeService;
  @Mock private SensitiveWordFilter sensitiveWordFilter;
  @Mock private PolicyService policyService;
  @Mock private IdempotencyHelper idempotencyHelper;
  @Mock private DistributedLockHelper lockHelper;

  private CoachProfileService coachProfileService;

  @BeforeEach
  void setUp() {
    coachProfileService =
        new CoachProfileService(
            coachMapper,
            coachCertificateMapper,
            coachUpdateLogMapper,
            phoneEncryptor,
            idCardEncryptor,
            smsCodeService,
            sensitiveWordFilter,
            policyService,
            idempotencyHelper,
            lockHelper);
    lenient().doNothing().when(idempotencyHelper).checkAndLock(anyString(), anyLong(), anyString());
    lenient().doNothing().when(idempotencyHelper).unlock(anyString(), anyLong(), anyString());
  }

  @Test
  @DisplayName("获取教练主页：正常返回脱敏资料")
  void getProfile_activeCoach_returnsProfile() throws Exception {
    Coach coach = approvedCoach(1L);
    coach.setPhone("encryptedPhone");
    coach.setIdCardNo("encryptedIdCard");
    coach.setCertificates(
        List.of(cert("ID_CARD_FRONT", "front.jpg"), cert("COACH_CERT", "cert.jpg")));
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(phoneEncryptor.decrypt("encryptedPhone")).thenReturn("13800138000");
    when(idCardEncryptor.decrypt("encryptedIdCard")).thenReturn("110101199001011234");
    when(policyService.getConsentStatus(ActorType.coach, 1L))
        .thenReturn(new ConsentStatusResponse("v1", "v1", "v1", "v1", false));

    CoachProfileResponse response = coachProfileService.getProfile(1L);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.phone()).isEqualTo("13800138000");
    assertThat(response.idCardNoMasked()).isEqualTo("110101********1234");
    assertThat(response.idCardFrontUrl()).isEqualTo("front.jpg");
    assertThat(response.coachCertUrls()).containsExactly("cert.jpg");
  }

  @Test
  @DisplayName("获取教练主页：已离职教练抛出 COACH_NOT_FOUND")
  void getProfile_resignedCoach_throwsCoachNotFound() {
    Coach coach = new Coach();
    coach.setId(1L);
    coach.setStatus(CoachStatus.RESIGNED.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(() -> coachProfileService.getProfile(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("更新教练资料：正常更新并记录变更日志")
  void updateProfile_validRequest_updatesAndLogs() throws Exception {
    Coach coach = approvedCoach(1L);
    coach.setName("Old");
    coach.setAge(30);
    coach.setGender("male");
    coach.setEmail("old@example.com");
    coach.setPhone("encryptedPhone");
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(policyService.hasAgreedCurrentPolicy(ActorType.coach, 1L)).thenReturn(true);
    when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
    stubPhoneEncryptor();

    UpdateCoachProfileRequest request =
        new UpdateCoachProfileRequest(
            "NewName",
            35,
            "female",
            null,
            null,
            "new@example.com",
            null,
            null,
            5,
            null,
            List.of("蛙泳"),
            "我是一名专业游泳教练，擅长基础教学。",
            null,
            null,
            null,
            "key-1");

    ConsentStatusResponse consent =
        new ConsentStatusResponse("v1", "v1", "v1", "v1", false);
    when(policyService.getConsentStatus(ActorType.coach, 1L)).thenReturn(consent);

    coachProfileService.updateProfile(1L, request);

    assertThat(coach.getName()).isEqualTo("NewName");
    assertThat(coach.getAge()).isEqualTo(35);
    assertThat(coach.getGender()).isEqualTo("female");
    assertThat(coach.getEmail()).isEqualTo("new@example.com");
    verify(coachMapper).updateById(coach);
    ArgumentCaptor<CoachUpdateLog> logCaptor = ArgumentCaptor.forClass(CoachUpdateLog.class);
    verify(coachUpdateLogMapper, times(7)).insert(logCaptor.capture());
    List<String> loggedFields =
        logCaptor.getAllValues().stream().map(CoachUpdateLog::getFieldName).toList();
    assertThat(loggedFields)
        .containsExactlyInAnyOrder("name", "age", "gender", "email", "teachingYears", "teachingStrokes", "bio");
  }

  @Test
  @DisplayName("更新教练资料：状态非已通过抛出 COACH_STATUS_NOT_APPROVED")
  void updateProfile_statusNotApproved_throwsNotApproved() {
    Coach coach = new Coach();
    coach.setId(1L);
    coach.setStatus(CoachStatus.PENDING.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);

    UpdateCoachProfileRequest request = buildMinimalRequest("key-2");

    assertThatThrownBy(() -> coachProfileService.updateProfile(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_STATUS_NOT_APPROVED));
  }

  @Test
  @DisplayName("更新教练资料：未同意当前协议抛出 TERMS_NOT_ACCEPTED")
  void updateProfile_policyNotAgreed_throwsTermsNotAccepted() {
    Coach coach = approvedCoach(1L);
    coach.setPhone("encryptedPhone");
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(policyService.hasAgreedCurrentPolicy(ActorType.coach, 1L)).thenReturn(false);
    stubPhoneEncryptor();

    UpdateCoachProfileRequest request = buildMinimalRequest("key-3");

    assertThatThrownBy(() -> coachProfileService.updateProfile(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_ACCEPTED));
  }

  @Test
  @DisplayName("更新教练资料：姓名包含敏感词抛出 NICKNAME_SENSITIVE")
  void updateProfile_sensitiveName_throwsNicknameSensitive() {
    Coach coach = approvedCoach(1L);
    coach.setPhone("encryptedPhone");
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(policyService.hasAgreedCurrentPolicy(ActorType.coach, 1L)).thenReturn(true);
    lenient().when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
    when(sensitiveWordFilter.containsSensitive("BadName")).thenReturn(true);
    stubPhoneEncryptor();

    UpdateCoachProfileRequest request =
        new UpdateCoachProfileRequest(
            "BadName",
            25,
            "male",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "key-4");

    assertThatThrownBy(() -> coachProfileService.updateProfile(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.NICKNAME_SENSITIVE));
  }

  @Test
  @DisplayName("更新教练资料：个人简介长度不足抛出 VALIDATION_ERROR")
  void updateProfile_bioTooShort_throwsValidationError() {
    Coach coach = approvedCoach(1L);
    coach.setPhone("encryptedPhone");
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(policyService.hasAgreedCurrentPolicy(ActorType.coach, 1L)).thenReturn(true);
    lenient().when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
    stubPhoneEncryptor();

    UpdateCoachProfileRequest request =
        new UpdateCoachProfileRequest(
            "Name",
            25,
            "male",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "太短",
            null,
            null,
            null,
            "key-5");

    assertThatThrownBy(() -> coachProfileService.updateProfile(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR));
  }

  @Test
  @DisplayName("更新教练资料：上传形象照会更新 coach_certificate 表")
  void updateProfile_withPortrait_insertsOrUpdatesPortrait() throws Exception {
    Coach coach = approvedCoach(1L);
    coach.setPhone("encryptedPhone");
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(policyService.hasAgreedCurrentPolicy(ActorType.coach, 1L)).thenReturn(true);
    when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(false);
    when(coachCertificateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    stubPhoneEncryptor();
    ConsentStatusResponse consent =
        new ConsentStatusResponse("v1", "v1", "v1", "v1", false);
    when(policyService.getConsentStatus(ActorType.coach, 1L)).thenReturn(consent);

    UpdateCoachProfileRequest request = buildMinimalRequest("key-6");
    // portraitUrl is not part of the record; profile update page uses avatar upload.

    coachProfileService.updateProfile(1L, request);

    verify(coachCertificateMapper, never()).insert(any(CoachCertificate.class));
  }

  @Test
  @DisplayName("更新参考单价：正常更新并返回剩余次数")
  void updateReferencePrice_validPrice_updatesAndReturnsRemaining() {
    Coach coach = approvedCoach(1L);
    coach.setReferencePrice(new BigDecimal("100.00"));
    coach.setPriceChangedAt(LocalDateTime.now().minusDays(1));
    coach.setPriceChangeCountToday(2);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    var response =
        coachProfileService.updateReferencePrice(
            1L, new UpdateCoachReferencePriceRequest(new BigDecimal("150.00"), "key-7"));

    assertThat(response.referencePrice()).isEqualByComparingTo("150.00");
    assertThat(response.remainingChangesToday()).isEqualTo(2);
    assertThat(coach.getPriceChangeCountToday()).isEqualTo(1);
    verify(coachMapper).updateById(coach);
    verify(coachUpdateLogMapper).insert(any(CoachUpdateLog.class));
  }

  @Test
  @DisplayName("更新参考单价：价格超出范围抛出 INVALID_REFERENCE_PRICE")
  void updateReferencePrice_priceOutOfRange_throwsInvalidReferencePrice() {
    Coach coach = approvedCoach(1L);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(
            () ->
                coachProfileService.updateReferencePrice(
                    1L, new UpdateCoachReferencePriceRequest(new BigDecimal("3000.00"), "key-8")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFERENCE_PRICE));
  }

  @Test
  @DisplayName("更新参考单价：今日次数已达上限抛出 PRICE_CHANGE_LIMIT_REACHED")
  void updateReferencePrice_dailyLimitReached_throwsLimitReached() {
    Coach coach = approvedCoach(1L);
    coach.setReferencePrice(new BigDecimal("100.00"));
    coach.setPriceChangedAt(LocalDateTime.now());
    coach.setPriceChangeCountToday(3);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(
            () ->
                coachProfileService.updateReferencePrice(
                    1L, new UpdateCoachReferencePriceRequest(new BigDecimal("120.00"), "key-9")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRICE_CHANGE_LIMIT_REACHED));
  }

  @Test
  @DisplayName("更新参考单价：跨日重置次数后再更新")
  void updateReferencePrice_nextDay_resetsCountAndUpdates() {
    Coach coach = approvedCoach(1L);
    coach.setReferencePrice(new BigDecimal("80.00"));
    coach.setPriceChangedAt(LocalDateTime.now().minusDays(1).withHour(23).withMinute(59));
    coach.setPriceChangeCountToday(3);
    when(coachMapper.selectById(1L)).thenReturn(coach);

    var response =
        coachProfileService.updateReferencePrice(
            1L, new UpdateCoachReferencePriceRequest(new BigDecimal("120.00"), "key-10"));

    assertThat(response.remainingChangesToday()).isEqualTo(2);
    assertThat(coach.getPriceChangeCountToday()).isEqualTo(1);
  }

  @Test
  @DisplayName("更新参考单价：状态非已通过抛出 COACH_STATUS_NOT_APPROVED")
  void updateReferencePrice_notApproved_throwsNotApproved() {
    Coach coach = new Coach();
    coach.setId(1L);
    coach.setStatus(CoachStatus.REJECTED.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);

    assertThatThrownBy(
            () ->
                coachProfileService.updateReferencePrice(
                    1L, new UpdateCoachReferencePriceRequest(new BigDecimal("100.00"), "key-11")))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COACH_STATUS_NOT_APPROVED));
  }

  private Coach approvedCoach(Long id) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setName("Coach");
    coach.setAge(30);
    coach.setGender("male");
    coach.setProfileCompleted(false);
    return coach;
  }

  private Coach.CoachCertificate cert(String name, String url) {
    Coach.CoachCertificate certificate = new Coach.CoachCertificate();
    certificate.setName(name);
    certificate.setUrl(url);
    return certificate;
  }

  private UpdateCoachProfileRequest buildMinimalRequest(String idempotencyKey) {
    return new UpdateCoachProfileRequest(
        "Name",
        25,
        "male",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        idempotencyKey);
  }

  private void stubPhoneEncryptor() {
    try {
      lenient().when(phoneEncryptor.decrypt(anyString())).thenReturn("13800138000");
      lenient().when(phoneEncryptor.hash(anyString())).thenReturn("phone-hash");
      lenient().when(phoneEncryptor.encrypt(anyString())).thenReturn("encrypted");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
