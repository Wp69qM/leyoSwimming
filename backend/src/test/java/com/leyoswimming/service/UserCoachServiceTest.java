package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachDetailRequest;
import com.leyoswimming.dto.request.CoachListRequest;
import com.leyoswimming.dto.response.CoachDetailResponse;
import com.leyoswimming.dto.response.CoachListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCoachServiceTest {

  @Mock private CoachMapper coachMapper;
  @Mock private CoachCertificateMapper certificateMapper;
  @Mock private PackageTemplateMapper packageTemplateMapper;
  @Mock private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Mock private PhoneEncryptor phoneEncryptor;

  private UserCoachService userCoachService;

  @BeforeEach
  void setUp() {
    userCoachService =
        new UserCoachService(
            coachMapper,
            certificateMapper,
            packageTemplateMapper,
            packageTemplateCoachMapper,
            phoneEncryptor);
  }

  @Test
  @DisplayName("list: 返回公开教练并按评分降序")
  void list_returnsPublicCoachesSortedByRatingDesc() {
    Coach c1 = publicCoach(1L, "王教练", new BigDecimal("4.9"), CoachStatus.APPROVED.getValue());
    Coach c2 = publicCoach(2L, "李教练", new BigDecimal("4.5"), CoachStatus.RESIGNING.getValue());
    Page<Coach> pageResult = new Page<>(1, 10, 2);
    pageResult.setRecords(List.of(c1, c2));
    when(coachMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);

    CoachListResponse response =
        userCoachService.list(new CoachListRequest(1, 10, null, null, null));

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).rating()).isEqualByComparingTo(new BigDecimal("4.9"));
    assertThat(response.items().get(1).status()).isEqualTo(CoachStatus.RESIGNING.getValue());
  }

  @Test
  @DisplayName("list: 无公开教练返回空列表")
  void list_noPublicCoaches_returnsEmpty() {
    Page<Coach> pageResult = new Page<>(1, 10, 0);
    pageResult.setRecords(List.of());
    when(coachMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);

    CoachListResponse response =
        userCoachService.list(new CoachListRequest(1, 10, null, null, null));

    assertThat(response.items()).isEmpty();
    assertThat(response.total()).isZero();
  }

  @Test
  @DisplayName("detail: 返回已通过教练详情")
  void detail_activeCoach_returnsDetail() {
    Coach coach = publicCoach(1L, "王教练", new BigDecimal("4.9"), CoachStatus.APPROVED.getValue());
    coach.setGender("男");
    coach.setAge(32);
    coach.setTotalStudents(128);
    coach.setTotalHours(2560);
    coach.setBio("专业游泳教练");
    coach.setReferencePrice(new BigDecimal("200.00"));
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(certificateMapper.findByCoachId(1L)).thenReturn(List.of());
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    CoachDetailResponse response = userCoachService.detail(new CoachDetailRequest(1L));

    assertThat(response.name()).isEqualTo("王教练");
    assertThat(response.gender()).isEqualTo(1);
    assertThat(response.totalStudents()).isEqualTo(128);
  }

  @Test
  @DisplayName("detail: 申请离职中教练可查看")
  void detail_resigningCoach_returnsDetail() {
    Coach coach =
        publicCoach(4L, "李教练", new BigDecimal("4.7"), CoachStatus.RESIGNING.getValue());
    when(coachMapper.selectById(4L)).thenReturn(coach);
    when(certificateMapper.findByCoachId(4L)).thenReturn(List.of());
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    CoachDetailResponse response = userCoachService.detail(new CoachDetailRequest(4L));

    assertThat(response.status()).isEqualTo(CoachStatus.RESIGNING.getValue());
  }

  @Test
  @DisplayName("detail: 待审核教练返回 COACH_NOT_FOUND")
  void detail_pendingCoach_notFound() {
    Coach coach = publicCoach(2L, "张教练", new BigDecimal("4.0"), CoachStatus.PENDING.getValue());
    when(coachMapper.selectById(2L)).thenReturn(coach);

    assertThatThrownBy(() -> userCoachService.detail(new CoachDetailRequest(2L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("detail: 已离职教练返回 COACH_NOT_FOUND")
  void detail_resignedCoach_notFound() {
    Coach coach = publicCoach(3L, "刘教练", new BigDecimal("4.0"), CoachStatus.RESIGNED.getValue());
    when(coachMapper.selectById(3L)).thenReturn(coach);

    assertThatThrownBy(() -> userCoachService.detail(new CoachDetailRequest(3L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("detail: 体验课套餐排在标准套餐前面")
  void detail_experiencePackageFirst() {
    Coach coach = publicCoach(1L, "王教练", new BigDecimal("5.0"), CoachStatus.APPROVED.getValue());
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(certificateMapper.findByCoachId(1L)).thenReturn(List.of());

    PackageTemplate standard = activeTemplate(10L, "正价课", "standard", new BigDecimal("1800.00"), 10);
    PackageTemplate experience =
        activeTemplate(11L, "体验课", "experience", new BigDecimal("99.00"), 1);

    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(
            List.of(link(10L, 1L, standard.getPrice()), link(11L, 1L, experience.getPrice())));
    when(packageTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(standard, experience));

    CoachDetailResponse response = userCoachService.detail(new CoachDetailRequest(1L));

    assertThat(response.packages()).hasSize(2);
    assertThat(response.packages().get(0).packageMode()).isEqualTo("experience");
    assertThat(response.packages().get(1).packageMode()).isEqualTo("standard");
  }

  private Coach publicCoach(Long id, String name, BigDecimal rating, int status) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setName(name);
    coach.setRating(rating);
    coach.setStatus(status);
    coach.setRealtimeStatus("空闲中");
    coach.setTeachingStrokes("蛙泳,自由泳");
    return coach;
  }

  private PackageTemplate activeTemplate(
      Long id, String name, String packageMode, BigDecimal price, int totalHours) {
    PackageTemplate template = new PackageTemplate();
    template.setId(id);
    template.setName(name);
    template.setPackageMode(packageMode);
    template.setPrice(price);
    template.setTotalHours(totalHours);
    template.setStatus("ACTIVE");
    return template;
  }

  private PackageTemplateCoach link(Long templateId, Long coachId, BigDecimal referencePrice) {
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(templateId);
    link.setCoachId(coachId);
    link.setReferencePriceSnapshot(referencePrice);
    return link;
  }
}
