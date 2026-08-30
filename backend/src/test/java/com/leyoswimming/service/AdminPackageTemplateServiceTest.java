package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminPackageTemplateAddRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateCustomConfigRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateListRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateToggleRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateUpdateRequest;
import com.leyoswimming.dto.response.AdminPackageTemplateDetailResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPackageTemplateServiceTest {

  @Mock private PackageTemplateMapper packageTemplateMapper;
  @Mock private PackageTemplateCoachMapper packageTemplateCoachMapper;
  @Mock private CustomPackageConfigMapper customPackageConfigMapper;
  @Mock private CoachMapper coachMapper;
  @Mock private AdminPermissionHelper permissionHelper;
  @Mock private IdempotencyHelper idempotencyHelper;

  private AdminPackageTemplateService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminPackageTemplateService(
            packageTemplateMapper,
            packageTemplateCoachMapper,
            customPackageConfigMapper,
            coachMapper,
            permissionHelper,
            idempotencyHelper);
    doNothing().when(permissionHelper).checkPermission(anyLong(), any());
    lenient().doNothing().when(idempotencyHelper).checkAndLock(any(), anyLong(), any());
    lenient()
        .when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coach(1L, "教练 A", new BigDecimal("300.00"))));
  }

  @Test
  @DisplayName("add: 合法标准套餐创建成功")
  void add_validStandardTemplate_returnsTemplate() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期 10 节课",
            "standard",
            List.of(1L, 2L),
            "one_on_one",
            List.of(1, 2),
            10,
            60,
            180,
            new BigDecimal("3600.00"),
            new BigDecimal("3000.00"),
            true,
            new BigDecimal("0.80"),
            30,
            List.of("热销"),
            "<p>暑期特惠</p>",
            List.of("https://cdn.example.com/a.jpg"),
            "uuid");

    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coach(1L, "教练 A", new BigDecimal("300.00")), coach(2L, "教练 B", new BigDecimal("350.00"))));
    when(packageTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(packageTemplateMapper.insert(any(PackageTemplate.class))).thenAnswer(
        inv -> {
          PackageTemplate t = inv.getArgument(0);
          t.setId(100L);
          return 1;
        });
    when(packageTemplateCoachMapper.findByTemplateId(100L))
        .thenReturn(List.of(coachLink(100L, 1L), coachLink(100L, 2L)));

    AdminPackageTemplateDetailResponse response = service.add(1L, request);

    assertThat(response.packageTemplateId()).isEqualTo(100L);
    assertThat(response.status()).isEqualTo("inactive");
    assertThat(response.coachIds()).containsExactly(1L, 2L);
    verify(packageTemplateMapper).insert(any(PackageTemplate.class));
  }

  @Test
  @DisplayName("add: 重复幂等键抛出 IDEMPOTENCY_DUPLICATE")
  void add_duplicateIdempotencyKey_throwsIdempotencyDuplicate() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期 10 节课",
            "standard",
            List.of(1L),
            "one_on_one",
            null,
            10,
            60,
            90,
            new BigDecimal("1000.00"),
            new BigDecimal("800.00"),
            false,
            null,
            null,
            null,
            null,
            null,
            "dup-key");

    doThrow(new BusinessException(ErrorCode.IDEMPOTENCY_DUPLICATE))
        .when(idempotencyHelper)
        .checkAndLock("admin_package_template", 1L, "dup-key");

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.IDEMPOTENCY_DUPLICATE));
    verify(packageTemplateMapper, never()).insert(any(PackageTemplate.class));
  }

  @Test
  @DisplayName("add: 名称为空抛出 INVALID_PACKAGE_PARAM")
  void add_blankName_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            " ", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
    verify(packageTemplateMapper, never()).insert(any(PackageTemplate.class));
  }

  @Test
  @DisplayName("add: 适用教练不存在抛出 INVALID_PACKAGE_PARAM")
  void add_nonExistentCoach_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(99L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
    verify(packageTemplateMapper, never()).insert(any(PackageTemplate.class));
  }

  @Test
  @DisplayName("add: 适用教练为空抛出 INVALID_PACKAGE_PARAM")
  void add_emptyCoachIds_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", Collections.emptyList(), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 课时数小于等于 0 抛出 INVALID_PACKAGE_PARAM")
  void add_nonPositiveHours_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "one_on_one", null, 0, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 售价负数抛出 INVALID_PACKAGE_PARAM")
  void add_negativePrice_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("-1.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 原价小于售价抛出 INVALID_PACKAGE_PARAM")
  void add_originalPriceLessThanPrice_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("500.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 启用退款但退款比例缺失抛出 INVALID_PACKAGE_PARAM")
  void add_refundEnabledWithoutRatio_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), true, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 名称重复抛出 DUPLICATE_PACKAGE_NAME")
  void add_duplicateName_throwsDuplicatePackageName() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期 10 节课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    when(packageTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_PACKAGE_NAME));
  }

  @Test
  @DisplayName("update: 已上架模板不可编辑")
  void update_activeTemplate_throwsActiveCannotEdit() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.ACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L, "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, 0);

    assertThatThrownBy(() -> service.update(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PACKAGE_TEMPLATE_ACTIVE_CANNOT_EDIT));
  }

  @Test
  @DisplayName("toggleStatus: active 切换为 inactive")
  void toggleStatus_active_becomesInactive() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.ACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.updateById(any(PackageTemplate.class))).thenReturn(1);
    when(packageTemplateCoachMapper.findByTemplateId(1L)).thenReturn(List.of());

    AdminPackageTemplateDetailResponse response =
        service.toggleStatus(1L, new AdminPackageTemplateToggleRequest(1L));

    assertThat(response.status()).isEqualTo("inactive");
    verify(packageTemplateMapper).updateById(existing);
  }

  @Test
  @DisplayName("toggleStatus: inactive 切换为 active")
  void toggleStatus_inactive_becomesActive() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.updateById(any(PackageTemplate.class))).thenReturn(1);
    when(packageTemplateCoachMapper.selectCountByTemplateId(1L)).thenReturn(1L);

    AdminPackageTemplateDetailResponse response =
        service.toggleStatus(1L, new AdminPackageTemplateToggleRequest(1L));

    assertThat(response.status()).isEqualTo("active");
  }

  @Test
  @DisplayName("customConfig: 合法配置保存成功")
  void customConfig_validConfig_returnsConfig() {
    when(customPackageConfigMapper.findFirst()).thenReturn(null);
    when(customPackageConfigMapper.insert(any(CustomPackageConfig.class))).thenAnswer(
        inv -> {
          CustomPackageConfig c = inv.getArgument(0);
          c.setId(1L);
          return 1;
        });

    var response =
        service.customConfig(1L, new AdminPackageTemplateCustomConfigRequest(5, 50, 180));

    assertThat(response.configId()).isEqualTo(1L);
    assertThat(response.minHours()).isEqualTo(5);
  }

  @Test
  @DisplayName("customConfig: minHours 小于等于 0 抛出 INVALID_CUSTOM_PACKAGE_CONFIG")
  void customConfig_nonPositiveMinHours_throwsInvalidConfig() {
    assertThatThrownBy(
            () ->
                service.customConfig(
                    1L, new AdminPackageTemplateCustomConfigRequest(0, 50, 180)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG));
  }

  @Test
  @DisplayName("customConfig: minHours 大于 maxHours 抛出 INVALID_CUSTOM_PACKAGE_CONFIG")
  void customConfig_minGreaterThanMax_throwsInvalidConfig() {
    assertThatThrownBy(
            () ->
                service.customConfig(
                    1L, new AdminPackageTemplateCustomConfigRequest(50, 5, 180)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG));
  }

  @Test
  @DisplayName("customConfig: maxHours 大于 100 抛出 INVALID_CUSTOM_PACKAGE_CONFIG")
  void customConfig_maxHoursOver100_throwsInvalidConfig() {
    assertThatThrownBy(
            () ->
                service.customConfig(
                    1L, new AdminPackageTemplateCustomConfigRequest(1, 101, 180)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG));
  }

  @Test
  @DisplayName("list: 返回分页结果")
  void list_withFilter_returnsPage() {
    PackageTemplate t = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    Page<PackageTemplate> page = new Page<>(1, 10);
    page.setRecords(List.of(t));
    page.setTotal(1);
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(page);
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coachLink(1L, 1L), coachLink(1L, 2L)));
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coach(1L, "教练 A", null), coach(2L, "教练 B", null)));

    AdminPackageTemplateListResponse response =
        service.list(1L, new AdminPackageTemplateListRequest(1, 10, null, null, null, null));

    assertThat(response.items()).hasSize(1);
    assertThat(response.total()).isEqualTo(1);
    assertThat(response.items().get(0).coachIds()).containsExactly(1L, 2L);
  }

  private PackageTemplate template(Long id, String name, PackageTemplateStatus status) {
    PackageTemplate t = new PackageTemplate();
    t.setId(id);
    t.setName(name);
    t.setPackageMode("standard");
    t.setTeachingType("one_on_one");
    t.setTotalHours(10);
    t.setDurationMinutes(60);
    t.setValidDays(90);
    t.setOriginalPrice(new BigDecimal("1000.00"));
    t.setPrice(new BigDecimal("800.00"));
    t.setRefundEnabled(false);
    t.setStatus(status.getValue());
    t.setVersion(0);
    return t;
  }

  private Coach coach(Long id, String name, BigDecimal referencePrice) {
    Coach c = new Coach();
    c.setId(id);
    c.setName(name);
    c.setReferencePrice(referencePrice);
    return c;
  }

  @Test
  @DisplayName("list: 带状态筛选返回分页结果")
  void list_withStatusFilter_appliesStatusFilter() {
    PackageTemplate t = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    Page<PackageTemplate> page = new Page<>(1, 10);
    page.setRecords(List.of(t));
    page.setTotal(1);
    when(packageTemplateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
        .thenReturn(page);
    when(packageTemplateCoachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    AdminPackageTemplateListResponse response =
        service.list(1L, new AdminPackageTemplateListRequest(1, 10, null, "inactive", null, null));

    assertThat(response.items()).hasSize(1);
  }

  @Test
  @DisplayName("detail: 模板不存在抛出 PACKAGE_TEMPLATE_NOT_FOUND")
  void detail_notFound_throwsNotFound() {
    when(packageTemplateMapper.selectById(99L)).thenReturn(null);

    assertThatThrownBy(() -> service.detail(1L, 99L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PACKAGE_TEMPLATE_NOT_FOUND));
  }

  @Test
  @DisplayName("add: 套餐模式非法抛出 INVALID_PACKAGE_PARAM")
  void add_invalidPackageMode_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "invalid", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 教学类型非法抛出 INVALID_PACKAGE_PARAM")
  void add_invalidTeachingType_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "invalid", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 名称超长抛出 INVALID_PACKAGE_PARAM")
  void add_nameTooLong_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "a".repeat(65), "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 适用教练包含非法 ID 抛出 INVALID_PACKAGE_PARAM")
  void add_invalidCoachId_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(0L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), false, null, null,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 退款比例超出 1 抛出 INVALID_PACKAGE_PARAM")
  void add_refundRatioOver1_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
            new BigDecimal("1000.00"), new BigDecimal("800.00"), true, new BigDecimal("1.50"), 30,
            null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("add: 退款有效期为负抛出 INVALID_PACKAGE_PARAM")
  void add_refundValidDaysNegative_throwsInvalidPackageParam() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
                "暑期课", "standard", List.of(1L), "one_on_one", null, 10, 60, 90,
                new BigDecimal("1000.00"), new BigDecimal("800.00"), true, new BigDecimal("0.80"), -1,
                null, null, null, null);

    assertThatThrownBy(() -> service.add(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
  }

  @Test
  @DisplayName("update: 版本号不匹配抛出 USER_CONCURRENTLY_UPDATED")
  void update_versionMismatch_throwsConcurrentlyUpdated() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L, null, null, List.of(1L), null, null, null, null, null, null, null, null, null, null,
            null, null, null, 99);

    assertThatThrownBy(() -> service.update(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_CONCURRENTLY_UPDATED));
  }

  @Test
  @DisplayName("update: updateById 返回 0 抛出 USER_CONCURRENTLY_UPDATED")
  void update_updateReturnsZero_throwsConcurrentlyUpdated() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.updateById(any(PackageTemplate.class))).thenReturn(0);

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L, null, null, List.of(1L), null, null, null, null, null, null, null, null, null, null,
            null, null, null, 0);

    assertThatThrownBy(() -> service.update(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_CONCURRENTLY_UPDATED));
  }

  @Test
  @DisplayName("update: 更新所有可选字段成功")
  void update_allOptionalFields_returnsUpdatedTemplate() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.updateById(any(PackageTemplate.class))).thenReturn(1);
    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coach(1L, "教练 A", new BigDecimal("300.00"))));
    when(packageTemplateCoachMapper.findByTemplateId(1L)).thenReturn(List.of(coachLink(1L, 1L)));

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L,
            "更新名称",
            "experience",
            List.of(1L),
            "one_on_two",
            List.of(2),
            12,
            90,
            180,
            new BigDecimal("2000.00"),
            new BigDecimal("1500.00"),
            true,
            new BigDecimal("0.70"),
            60,
            List.of("标签"),
            "描述",
            List.of("https://cdn.example.com/b.jpg"),
            0);

    AdminPackageTemplateDetailResponse response = service.update(1L, request);

    assertThat(response.name()).isEqualTo("更新名称");
    assertThat(response.packageMode()).isEqualTo("experience");
    assertThat(response.teachingType()).isEqualTo("one_on_two");
    assertThat(response.totalHours()).isEqualTo(12);
  }

  @Test
  @DisplayName("update: 更新名称重复抛出 DUPLICATE_PACKAGE_NAME")
  void update_duplicateName_throwsDuplicatePackageName() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L, "重复名称", null, List.of(1L), null, null, null, null, null, null, null, null, null,
            null, null, null, null, 0);

    assertThatThrownBy(() -> service.update(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_PACKAGE_NAME));
  }

  @Test
  @DisplayName("update: 适用教练不存在抛出 INVALID_PACKAGE_PARAM")
  void update_nonExistentCoach_throwsInvalidPackageParam() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(coachMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

    AdminPackageTemplateUpdateRequest request =
        new AdminPackageTemplateUpdateRequest(
            1L, null, null, List.of(99L), null, null, null, null, null, null, null, null, null,
            null, null, null, null, 0);

    assertThatThrownBy(() -> service.update(1L, request))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_PACKAGE_PARAM));
    verify(packageTemplateMapper, never()).updateById(any(PackageTemplate.class));
  }

  @Test
  @DisplayName("toggleStatus: updateById 返回 0 抛出 USER_CONCURRENTLY_UPDATED")
  void toggleStatus_updateReturnsZero_throwsConcurrentlyUpdated() {
    PackageTemplate existing = template(1L, "暑期课", PackageTemplateStatus.INACTIVE);
    when(packageTemplateMapper.selectById(1L)).thenReturn(existing);
    when(packageTemplateMapper.updateById(any(PackageTemplate.class))).thenReturn(0);
    when(packageTemplateCoachMapper.selectCountByTemplateId(1L)).thenReturn(1L);

    assertThatThrownBy(() -> service.toggleStatus(1L, new AdminPackageTemplateToggleRequest(1L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_CONCURRENTLY_UPDATED));
  }

  @Test
  @DisplayName("customConfig: 已存在记录时更新成功")
  void customConfig_existing_updatesConfig() {
    CustomPackageConfig existing = new CustomPackageConfig();
    existing.setId(2L);
    existing.setMinHours(1);
    existing.setMaxHours(10);
    existing.setDefaultValidDays(30);
    when(customPackageConfigMapper.findFirst()).thenReturn(existing);
    when(customPackageConfigMapper.updateById(any(CustomPackageConfig.class))).thenReturn(1);

    var response =
        service.customConfig(1L, new AdminPackageTemplateCustomConfigRequest(5, 50, 180));

    assertThat(response.configId()).isEqualTo(2L);
    assertThat(response.minHours()).isEqualTo(5);
  }

  @Test
  @DisplayName("customConfig: defaultValidDays 小于等于 0 抛出 INVALID_CUSTOM_PACKAGE_CONFIG")
  void customConfig_nonPositiveDefaultValidDays_throwsInvalidConfig() {
    assertThatThrownBy(
            () ->
                service.customConfig(
                    1L, new AdminPackageTemplateCustomConfigRequest(5, 50, 0)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG));
  }

  @Test
  @DisplayName("add: 空图片列表和空描述可正常创建")
  void add_emptyImagesAndDescription_returnsTemplate() {
    AdminPackageTemplateAddRequest request =
        new AdminPackageTemplateAddRequest(
            "暑期 10 节课",
            "standard",
            List.of(1L),
            "one_on_one",
            null,
            10,
            60,
            180,
            new BigDecimal("3600.00"),
            new BigDecimal("3000.00"),
            false,
            null,
            null,
            null,
            null,
            Collections.emptyList(),
            null);

    when(coachMapper.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(List.of(coach(1L, "教练 A", new BigDecimal("300.00"))));
    when(packageTemplateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(packageTemplateMapper.insert(any(PackageTemplate.class))).thenAnswer(
        inv -> {
          PackageTemplate t = inv.getArgument(0);
          t.setId(101L);
          return 1;
        });
    when(packageTemplateCoachMapper.findByTemplateId(101L))
        .thenReturn(List.of(coachLink(101L, 1L)));

    AdminPackageTemplateDetailResponse response = service.add(1L, request);

    assertThat(response.packageTemplateId()).isEqualTo(101L);
    assertThat(response.images()).isEmpty();
  }

  private PackageTemplateCoach coachLink(Long templateId, Long coachId) {
    PackageTemplateCoach link = new PackageTemplateCoach();
    link.setPackageTemplateId(templateId);
    link.setCoachId(coachId);
    return link;
  }
}
