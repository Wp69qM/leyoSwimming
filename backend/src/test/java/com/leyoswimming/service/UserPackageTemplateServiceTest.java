package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachPackageListRequest;
import com.leyoswimming.dto.request.PackageDetailRequest;
import com.leyoswimming.dto.request.PackageListRequest;
import com.leyoswimming.dto.response.CoachPackageListResponse;
import com.leyoswimming.dto.response.PackageDetailResponse;
import com.leyoswimming.dto.response.PackageListItemResponse;
import com.leyoswimming.dto.response.PackageListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPackageTemplateServiceTest {

  @Mock private PackageTemplateMapper packageTemplateMapper;
  @Mock private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Mock private CoachMapper coachMapper;
  @Mock private CoachCertificateMapper coachCertificateMapper;
  @Mock private CustomPackageConfigMapper customPackageConfigMapper;

  private UserPackageTemplateService service;

  @BeforeEach
  void setUp() {
    service =
        new UserPackageTemplateService(
            packageTemplateMapper,
            packageTemplateCoachMapper,
            coachMapper,
            coachCertificateMapper,
            customPackageConfigMapper);
  }

  @Test
  @DisplayName("list: 返回已上架套餐并按创建时间倒序")
  void list_returnsActiveTemplates() {
    PackageTemplate t1 = activeTemplate(1L, "标准 6 节", "standard", 6);
    PackageTemplate t2 = activeTemplate(2L, "体验课", "experience", 1);
    Page<PackageTemplate> pageResult = new Page<>(1, 10, 2);
    pageResult.setRecords(List.of(t1, t2));
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);

    PackageListResponse response = service.list(new PackageListRequest(1, 10));

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).id()).isEqualTo(1L);
    assertThat(response.total()).isEqualTo(2L);
  }

  @Test
  @DisplayName("list: 无已上架套餐返回空列表")
  void list_noActiveTemplates_returnsEmpty() {
    Page<PackageTemplate> pageResult = new Page<>(1, 10, 0);
    pageResult.setRecords(List.of());
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);

    PackageListResponse response = service.list(new PackageListRequest(1, 10));

    assertThat(response.items()).isEmpty();
    assertThat(response.total()).isZero();
  }

  @Test
  @DisplayName("list: 存在全局配置和公开教练时合成自定义套餐项")
  void list_withConfigAndCoach_includesCustomPackage() {
    PackageTemplate t1 = activeTemplate(1L, "标准 6 节", "standard", 6);
    Page<PackageTemplate> pageResult = new Page<>(1, 10, 1);
    pageResult.setRecords(List.of(t1));
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfigWithId(100L, 1, 50));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(approvedCoach(1L, new BigDecimal("200.00"))));

    PackageListResponse response = service.list(new PackageListRequest(1, 10));

    assertThat(response.items()).hasSize(2);
    assertThat(response.total()).isEqualTo(2L);
    PackageListItemResponse custom = response.items().get(1);
    assertThat(custom.id()).isEqualTo(-1L);
    assertThat(custom.name()).isEqualTo("自定义课时");
    assertThat(custom.packageMode()).isEqualTo("custom");
  }

  @Test
  @DisplayName("list: 无全局配置时不合成自定义套餐项")
  void list_withoutConfig_excludesCustomPackage() {
    PackageTemplate t1 = activeTemplate(1L, "标准 6 节", "standard", 6);
    Page<PackageTemplate> pageResult = new Page<>(1, 10, 1);
    pageResult.setRecords(List.of(t1));
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);
    when(customPackageConfigMapper.findFirst()).thenReturn(null);

    PackageListResponse response = service.list(new PackageListRequest(1, 10));

    assertThat(response.items()).hasSize(1);
    assertThat(response.total()).isEqualTo(1L);
  }

  @Test
  @DisplayName("list: 无公开教练设参考单价时不合成自定义套餐项")
  void list_withoutPublicCoach_excludesCustomPackage() {
    PackageTemplate t1 = activeTemplate(1L, "标准 6 节", "standard", 6);
    Page<PackageTemplate> pageResult = new Page<>(1, 10, 1);
    pageResult.setRecords(List.of(t1));
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(pageResult);
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfigWithId(100L, 1, 50));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    PackageListResponse response = service.list(new PackageListRequest(1, 10));

    assertThat(response.items()).hasSize(1);
    assertThat(response.total()).isEqualTo(1L);
  }

  @Test
  @DisplayName("coachPackages: 返回教练支持的标准套餐和自定义入口")
  void coachPackages_validCoach_returnsPackages() {
    Coach coach = approvedCoach(1L, new BigDecimal("200.00"));
    when(coachMapper.selectById(1L)).thenReturn(coach);

    PackageTemplate t1 = activeTemplate(10L, "标准 6 节", "standard", 6);
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(link(10L, 1L)));
    when(packageTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(t1));
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfig(1, 50));

    CoachPackageListResponse response = service.coachPackages(new CoachPackageListRequest(1L));

    assertThat(response.coachId()).isEqualTo(1L);
    assertThat(response.standardPackages()).hasSize(1);
    assertThat(response.customPackageEnabled()).isTrue();
    assertThat(response.customHoursMax()).isEqualTo(50);
  }

  @Test
  @DisplayName("coachPackages: 教练未设参考单价时自定义入口禁用")
  void coachPackages_noReferencePrice_customDisabled() {
    Coach coach = approvedCoach(1L, null);
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(link(10L, 1L)));
    when(packageTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(activeTemplate(10L, "标准 6 节", "standard", 6)));

    CoachPackageListResponse response = service.coachPackages(new CoachPackageListRequest(1L));

    assertThat(response.customPackageEnabled()).isFalse();
  }

  @Test
  @DisplayName("coachPackages: 待审核教练返回 COACH_NOT_FOUND")
  void coachPackages_pendingCoach_notFound() {
    when(coachMapper.selectById(2L)).thenReturn(coachWithStatus(2L, CoachStatus.PENDING.getValue()));

    assertThatThrownBy(() -> service.coachPackages(new CoachPackageListRequest(2L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.COACH_NOT_FOUND));
  }

  @Test
  @DisplayName("coachPackages: 无标准套餐时返回空标准列表和自定义入口")
  void coachPackages_emptyStandard_returnsCustomOnly() {
    Coach coach = approvedCoach(1L, new BigDecimal("200.00"));
    when(coachMapper.selectById(1L)).thenReturn(coach);
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of());
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfig(2, 30));

    CoachPackageListResponse response = service.coachPackages(new CoachPackageListRequest(1L));

    assertThat(response.standardPackages()).isEmpty();
    assertThat(response.customPackageEnabled()).isTrue();
    assertThat(response.customHoursMin()).isEqualTo(2);
  }

  @Test
  @DisplayName("detail: 返回模板详情和全部适用教练")
  void detail_withoutCoachId_returnsAllCoaches() {
    PackageTemplate template = activeTemplate(10L, "标准 6 节", "standard", 6);
    when(packageTemplateMapper.selectById(10L)).thenReturn(template);
    when(packageTemplateCoachMapper.findByTemplateId(10L))
        .thenReturn(List.of(link(10L, 1L), link(10L, 2L)));
    Coach c1 = approvedCoach(1L, new BigDecimal("200.00"));
    Coach c2 = approvedCoach(2L, new BigDecimal("180.00"));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));

    PackageDetailResponse response = service.detail(new PackageDetailRequest(10L, null));

    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.applicableCoaches()).hasSize(2);
  }

  @Test
  @DisplayName("detail: 传入 coachId 仅返回选中教练")
  void detail_withCoachId_returnsSelectedCoach() {
    PackageTemplate template = activeTemplate(10L, "标准 6 节", "standard", 6);
    when(packageTemplateMapper.selectById(10L)).thenReturn(template);
    when(packageTemplateCoachMapper.findByTemplateId(10L))
        .thenReturn(List.of(link(10L, 1L), link(10L, 2L)));
    Coach c1 = approvedCoach(1L, new BigDecimal("200.00"));
    when(coachMapper.selectById(1L)).thenReturn(c1);
    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1));

    PackageDetailResponse response = service.detail(new PackageDetailRequest(10L, 1L));

    assertThat(response.applicableCoaches()).hasSize(1);
    assertThat(response.applicableCoaches().get(0).coachId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("detail: 未传 coachId 时过滤非公开状态教练")
  void detail_withoutCoachId_filtersNonPublicCoaches() {
    PackageTemplate template = activeTemplate(10L, "标准 6 节", "standard", 6);
    when(packageTemplateMapper.selectById(10L)).thenReturn(template);
    when(packageTemplateCoachMapper.findByTemplateId(10L))
        .thenReturn(List.of(link(10L, 1L), link(10L, 2L), link(10L, 3L)));
    Coach approved = approvedCoach(1L, new BigDecimal("200.00"));
    Coach resigning = coachWithStatus(2L, CoachStatus.RESIGNING.getValue());
    resigning.setName("离职中教练");
    Coach pending = coachWithStatus(3L, CoachStatus.PENDING.getValue());
    pending.setName("待审核教练");
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(approved, resigning, pending));

    PackageDetailResponse response = service.detail(new PackageDetailRequest(10L, null));

    assertThat(response.applicableCoaches()).hasSize(2);
    assertThat(response.applicableCoaches().stream().map(c -> c.coachId()).toList())
        .containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  @DisplayName("detail: 已下架套餐返回 PACKAGE_NOT_FOUND")
  void detail_inactiveTemplate_notFound() {
    PackageTemplate template = activeTemplate(10L, "标准 6 节", "standard", 6);
    template.setStatus("inactive");
    when(packageTemplateMapper.selectById(10L)).thenReturn(template);

    assertThatThrownBy(() -> service.detail(new PackageDetailRequest(10L, null)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_FOUND));
  }

  @Test
  @DisplayName("detail: 选中教练未适配套餐返回 PACKAGE_NOT_FOUND")
  void detail_coachNotApplicable_notFound() {
    PackageTemplate template = activeTemplate(10L, "标准 6 节", "standard", 6);
    when(packageTemplateMapper.selectById(10L)).thenReturn(template);
    when(packageTemplateCoachMapper.findByTemplateId(10L))
        .thenReturn(List.of(link(10L, 1L)));
    Coach c2 = approvedCoach(2L, new BigDecimal("180.00"));
    when(coachMapper.selectById(2L)).thenReturn(c2);

    assertThatThrownBy(() -> service.detail(new PackageDetailRequest(10L, 2L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_FOUND));
  }

  @Test
  @DisplayName("detail: 自定义套餐 ID 返回合成详情和全部适用教练")
  void detail_customPackageId_returnsSyntheticDetail() {
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfigWithId(100L, 1, 50));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(
            approvedCoach(1L, new BigDecimal("200.00")),
            approvedCoach(2L, new BigDecimal("180.00"))));

    PackageDetailResponse response = service.detail(new PackageDetailRequest(-1L, null));

    assertThat(response.id()).isEqualTo(-1L);
    assertThat(response.packageMode()).isEqualTo("custom");
    assertThat(response.applicableCoaches()).hasSize(2);
  }

  @Test
  @DisplayName("detail: 自定义套餐传入 coachId 仅返回选中教练")
  void detail_customPackageWithCoachId_returnsSelectedCoach() {
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfigWithId(100L, 1, 50));
    Coach c1 = approvedCoach(1L, new BigDecimal("200.00"));
    Coach c2 = approvedCoach(2L, new BigDecimal("180.00"));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(c1, c2));
    when(coachMapper.selectById(1L)).thenReturn(c1);

    PackageDetailResponse response = service.detail(new PackageDetailRequest(-1L, 1L));

    assertThat(response.id()).isEqualTo(-1L);
    assertThat(response.applicableCoaches()).hasSize(1);
    assertThat(response.applicableCoaches().get(0).coachId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("detail: 自定义套餐无公开教练返回 PACKAGE_NOT_FOUND")
  void detail_customPackageNoCoach_notFound() {
    when(customPackageConfigMapper.findFirst()).thenReturn(customConfigWithId(100L, 1, 50));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    assertThatThrownBy(() -> service.detail(new PackageDetailRequest(-1L, null)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PACKAGE_NOT_FOUND));
  }

  private Coach approvedCoach(Long id, BigDecimal referencePrice) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setName("教练" + id);
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coach.setReferencePrice(referencePrice);
    coach.setRating(new BigDecimal("4.9"));
    coach.setTeachingYears(5);
    coach.setTotalStudents(100);
    coach.setAvatarUrl("https://example.com/avatar" + id + ".png");
    coach.setTeachingStrokes("蛙泳,自由泳");
    return coach;
  }

  private Coach coachWithStatus(Long id, int status) {
    Coach coach = new Coach();
    coach.setId(id);
    coach.setStatus(status);
    return coach;
  }

  private PackageTemplate activeTemplate(Long id, String name, String packageMode, int totalHours) {
    PackageTemplate template = new PackageTemplate();
    template.setId(id);
    template.setName(name);
    template.setPackageMode(packageMode);
    template.setStatus("active");
    template.setTotalHours(totalHours);
    template.setDurationMinutes(60);
    template.setValidDays(30);
    template.setOriginalPrice(new BigDecimal("1200.00"));
    template.setPrice(new BigDecimal("1080.00"));
    template.setRefundEnabled(false);
    template.setTags(List.of("热销"));
    return template;
  }

  private PackageTemplateCoach link(Long templateId, Long coachId) {
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(templateId);
    link.setCoachId(coachId);
    return link;
  }

  private CustomPackageConfig customConfig(int minHours, int maxHours) {
    CustomPackageConfig config = new CustomPackageConfig();
    config.setMinHours(minHours);
    config.setMaxHours(maxHours);
    config.setDefaultValidDays(30);
    return config;
  }

  private CustomPackageConfig customConfigWithId(Long id, int minHours, int maxHours) {
    CustomPackageConfig config = customConfig(minHours, maxHours);
    config.setId(id);
    return config;
  }
}
