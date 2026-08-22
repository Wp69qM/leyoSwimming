package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminPackageTemplateAddRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateCustomConfigRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateListRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateToggleRequest;
import com.leyoswimming.dto.request.AdminPackageTemplateUpdateRequest;
import com.leyoswimming.dto.response.AdminPackageTemplateCustomConfigResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateDetailResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateListItemResponse;
import com.leyoswimming.dto.response.AdminPackageTemplateListResponse;
import com.leyoswimming.dto.response.CoachBriefResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.enums.TeachingType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.util.HtmlUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPackageTemplateService {

  private static final int MAX_NAME_LENGTH = 64;
  private static final int MAX_TAG_LENGTH = 20;
  private static final int MAX_HOURS = 100;
  private static final BigDecimal MAX_REFUND_RATIO = new BigDecimal("100");
  private static final String TAG_PATTERN = "[\\u4e00-\\u9fa5a-zA-Z0-9_-]+";
  private static final String DEFAULT_PACKAGE_MODE = PackageMode.STANDARD.getValue();
  private static final String DEFAULT_TEACHING_TYPE = TeachingType.ONE_ON_ONE.getValue();
  private static final String DEFAULT_STATUS = PackageTemplateStatus.INACTIVE.getValue();
  private static final String IDEMPOTENCY_ACTOR = "admin_package_template";
  private static final String CUSTOM_CONFIG_KEY = "global";

  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageTemplateCoachMapper packageTemplateCoachMapper;
  private final CustomPackageConfigMapper customPackageConfigMapper;
  private final CoachMapper coachMapper;
  private final AdminPermissionHelper permissionHelper;
  private final IdempotencyHelper idempotencyHelper;

  @Transactional(readOnly = true)
  public AdminPackageTemplateListResponse list(Long adminId, AdminPackageTemplateListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_READ);

    LambdaQueryWrapper<PackageTemplate> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.status())) {
      wrapper.eq(PackageTemplate::getStatus, request.status().trim());
    }

    if (StringUtils.isNotBlank(request.packageMode())) {
      wrapper.eq(PackageTemplate::getPackageMode, request.packageMode().trim());
    }

    if (StringUtils.isNotBlank(request.keyword())) {
      wrapper.like(PackageTemplate::getName, request.keyword().trim());
    }

    if (request.coachId() != null && request.coachId() > 0) {
      List<Long> templateIds = findTemplateIdsByCoachId(request.coachId());
      if (CollectionUtils.isEmpty(templateIds)) {
        return new AdminPackageTemplateListResponse(List.of(), 0, request.page(), request.pageSize());
      }
      wrapper.in(PackageTemplate::getId, templateIds);
    }

    wrapper.orderByDesc(PackageTemplate::getCreatedAt);

    Page<PackageTemplate> page = new Page<>(request.page(), request.pageSize());
    Page<PackageTemplate> result = packageTemplateMapper.selectPage(page, wrapper);

    Set<Long> templateIds = result.getRecords().stream()
        .map(PackageTemplate::getId)
        .collect(Collectors.toSet());
    Map<Long, List<PackageTemplateCoach>> coachesByTemplate = findCoachesByTemplateIds(templateIds);
    Set<Long> coachIds = coachesByTemplate.values().stream()
        .flatMap(List::stream)
        .map(PackageTemplateCoach::getCoachId)
        .collect(Collectors.toSet());
    Map<Long, Coach> coachMap = findCoachMap(coachIds);

    List<AdminPackageTemplateListItemResponse> items = result.getRecords().stream()
        .map(t -> toListItem(t, coachesByTemplate.getOrDefault(t.getId(), List.of()), coachMap))
        .toList();

    return new AdminPackageTemplateListResponse(
        items, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public AdminPackageTemplateDetailResponse detail(Long adminId, Long packageTemplateId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_READ);
    PackageTemplate template = findTemplate(packageTemplateId);
    return toDetailResponse(template);
  }

  @Transactional
  public AdminPackageTemplateDetailResponse add(Long adminId, AdminPackageTemplateAddRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);
    validateAddRequest(request);

    String idempotencyKey =
        StringUtils.isNotBlank(request.idempotencyKey()) ? request.idempotencyKey().trim() : null;
    if (idempotencyKey != null) {
      idempotencyHelper.checkAndLock(IDEMPOTENCY_ACTOR, adminId, idempotencyKey);
    }

    try {
      PackageTemplate template = new PackageTemplate();
      template.setName(request.name().trim());
      template.setPackageMode(resolvePackageMode(request.packageMode()));
      template.setTeachingType(resolveTeachingType(request.teachingType()));
      template.setStrokeIds(request.strokeIds());
      template.setTotalHours(request.totalHours());
      template.setDurationMinutes(request.durationMinutes());
      template.setValidDays(request.validDays());
      template.setOriginalPrice(request.originalPrice());
      template.setPrice(request.price());
      template.setRefundEnabled(request.refundEnabled());
      template.setRefundRatio(request.refundRatio());
      template.setRefundValidDays(request.refundValidDays());
      template.setTags(request.tags());
      template.setDescription(
          StringUtils.trimToNull(HtmlUtils.sanitizeDescription(request.description())));
      template.setImages(CollectionUtils.isEmpty(request.images()) ? null : request.images());
      template.setStatus(DEFAULT_STATUS);

      packageTemplateMapper.insert(template);

      saveCoachLinks(template.getId(), request.coachIds());

      return toDetailResponse(template);
    } catch (DuplicateKeyException e) {
      if (idempotencyKey != null) {
        idempotencyHelper.unlock(IDEMPOTENCY_ACTOR, adminId, idempotencyKey);
      }
      throw new BusinessException(ErrorCode.DUPLICATE_PACKAGE_NAME);
    } catch (RuntimeException e) {
      if (idempotencyKey != null) {
        idempotencyHelper.unlock(IDEMPOTENCY_ACTOR, adminId, idempotencyKey);
      }
      throw e;
    }
  }

  @Transactional
  public AdminPackageTemplateDetailResponse update(Long adminId, AdminPackageTemplateUpdateRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);
    PackageTemplate template = findTemplate(request.packageTemplateId());

    if (PackageTemplateStatus.ACTIVE.getValue().equals(template.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_TEMPLATE_ACTIVE_CANNOT_EDIT);
    }
    if (!Objects.equals(template.getVersion(), request.version())) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    validateUpdateRequest(request, template);

    if (StringUtils.isNotBlank(request.name())) {
      template.setName(request.name().trim());
    }
    if (StringUtils.isNotBlank(request.packageMode())) {
      template.setPackageMode(resolvePackageMode(request.packageMode()));
    }
    if (StringUtils.isNotBlank(request.teachingType())) {
      template.setTeachingType(resolveTeachingType(request.teachingType()));
    }
    if (request.strokeIds() != null) {
      template.setStrokeIds(request.strokeIds());
    }
    if (request.totalHours() != null) {
      template.setTotalHours(request.totalHours());
    }
    if (request.durationMinutes() != null) {
      template.setDurationMinutes(request.durationMinutes());
    }
    if (request.validDays() != null) {
      template.setValidDays(request.validDays());
    }
    if (request.originalPrice() != null) {
      template.setOriginalPrice(request.originalPrice());
    }
    if (request.price() != null) {
      template.setPrice(request.price());
    }
    if (request.refundEnabled() != null) {
      template.setRefundEnabled(request.refundEnabled());
    }
    if (request.refundRatio() != null) {
      template.setRefundRatio(request.refundRatio());
    }
    if (request.refundValidDays() != null) {
      template.setRefundValidDays(request.refundValidDays());
    }
    if (request.tags() != null) {
      template.setTags(request.tags());
    }
    if (request.description() != null) {
      template.setDescription(
          StringUtils.trimToNull(HtmlUtils.sanitizeDescription(request.description())));
    }
    if (request.images() != null) {
      template.setImages(CollectionUtils.isEmpty(request.images()) ? null : request.images());
    }

    int affected;
    try {
      affected = packageTemplateMapper.updateById(template);
    } catch (DuplicateKeyException e) {
      throw new BusinessException(ErrorCode.DUPLICATE_PACKAGE_NAME);
    }
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    saveCoachLinks(template.getId(), request.coachIds());

    return toDetailResponse(template);
  }

  @Transactional
  public AdminPackageTemplateDetailResponse toggleStatus(Long adminId, AdminPackageTemplateToggleRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);
    PackageTemplate template = findTemplate(request.packageTemplateId());

    String newStatus = PackageTemplateStatus.ACTIVE.getValue().equals(template.getStatus())
        ? PackageTemplateStatus.INACTIVE.getValue()
        : PackageTemplateStatus.ACTIVE.getValue();
    if (PackageTemplateStatus.ACTIVE.getValue().equals(newStatus)) {
      Long coachCount = packageTemplateCoachMapper.selectCountByTemplateId(template.getId());
      if (coachCount == null || coachCount == 0) {
        throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "上架前需至少关联 1 名教练");
      }
    }
    template.setStatus(newStatus);

    int affected = packageTemplateMapper.updateById(template);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.USER_CONCURRENTLY_UPDATED);
    }

    return toDetailResponse(template);
  }

  @Transactional(readOnly = true)
  public AdminPackageTemplateCustomConfigResponse customConfigDetail(Long adminId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_READ);
    CustomPackageConfig existing = customPackageConfigMapper.findFirst();
    if (existing == null) {
      return new AdminPackageTemplateCustomConfigResponse(null, null, null, null);
    }
    return new AdminPackageTemplateCustomConfigResponse(
        existing.getId(), existing.getMinHours(), existing.getMaxHours(), existing.getDefaultValidDays());
  }

  @Transactional
  public AdminPackageTemplateCustomConfigResponse customConfig(
      Long adminId, AdminPackageTemplateCustomConfigRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);
    validateCustomConfig(request);

    CustomPackageConfig existing = customPackageConfigMapper.findFirst();
    if (existing != null) {
      existing.setMinHours(request.minHours());
      existing.setMaxHours(request.maxHours());
      existing.setDefaultValidDays(request.defaultValidDays());
      customPackageConfigMapper.updateById(existing);
      return new AdminPackageTemplateCustomConfigResponse(
          existing.getId(), existing.getMinHours(), existing.getMaxHours(), existing.getDefaultValidDays());
    }

    CustomPackageConfig config = new CustomPackageConfig();
    config.setConfigKey(CUSTOM_CONFIG_KEY);
    config.setMinHours(request.minHours());
    config.setMaxHours(request.maxHours());
    config.setDefaultValidDays(request.defaultValidDays());
    customPackageConfigMapper.insert(config);
    return new AdminPackageTemplateCustomConfigResponse(
        config.getId(), config.getMinHours(), config.getMaxHours(), config.getDefaultValidDays());
  }

  private PackageTemplate findTemplate(Long packageTemplateId) {
    PackageTemplate template = packageTemplateMapper.selectById(packageTemplateId);
    if (template == null) {
      throw new BusinessException(ErrorCode.PACKAGE_TEMPLATE_NOT_FOUND);
    }
    return template;
  }

  private void validateAddRequest(AdminPackageTemplateAddRequest request) {
    if (StringUtils.isBlank(request.name()) || request.name().trim().length() > MAX_NAME_LENGTH) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "套餐名称不能为空且不超过 64 个字符");
    }
    if (!PackageMode.isValid(request.packageMode())) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "套餐模式错误");
    }
    if (!TeachingType.isValid(request.teachingType())) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "教学类型错误");
    }
    validateCommonFields(
        request.coachIds(),
        request.totalHours(),
        request.durationMinutes(),
        request.validDays(),
        request.originalPrice(),
        request.price(),
        request.refundEnabled(),
        request.refundRatio(),
        request.refundValidDays());
    validateCoachIdsExist(request.coachIds());
    validateTags(request.tags());

    if (nameExists(request.name().trim(), null)) {
      throw new BusinessException(ErrorCode.DUPLICATE_PACKAGE_NAME);
    }
  }

  private void validateUpdateRequest(AdminPackageTemplateUpdateRequest request, PackageTemplate existing) {
    if (StringUtils.isNotBlank(request.name()) && request.name().trim().length() > MAX_NAME_LENGTH) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "套餐名称不能超过 64 个字符");
    }
    if (StringUtils.isNotBlank(request.packageMode()) && !PackageMode.isValid(request.packageMode())) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "套餐模式错误");
    }
    if (StringUtils.isNotBlank(request.teachingType()) && !TeachingType.isValid(request.teachingType())) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "教学类型错误");
    }

    List<Long> coachIds = request.coachIds();
    Integer totalHours = request.totalHours() != null ? request.totalHours() : existing.getTotalHours();
    Integer durationMinutes = request.durationMinutes() != null ? request.durationMinutes() : existing.getDurationMinutes();
    Integer validDays = request.validDays() != null ? request.validDays() : existing.getValidDays();
    BigDecimal originalPrice = request.originalPrice() != null ? request.originalPrice() : existing.getOriginalPrice();
    BigDecimal price = request.price() != null ? request.price() : existing.getPrice();
    Boolean refundEnabled = request.refundEnabled() != null ? request.refundEnabled() : existing.getRefundEnabled();
    BigDecimal refundRatio = request.refundRatio() != null ? request.refundRatio() : existing.getRefundRatio();
    Integer refundValidDays = request.refundValidDays() != null ? request.refundValidDays() : existing.getRefundValidDays();

    validateCommonFields(coachIds, totalHours, durationMinutes, validDays, originalPrice, price, refundEnabled, refundRatio, refundValidDays);
    validateCoachIdsExist(coachIds);
    validateTags(request.tags());

    String name = StringUtils.isNotBlank(request.name()) ? request.name().trim() : existing.getName();
    if (StringUtils.isNotBlank(request.name()) && nameExists(name, existing.getId())) {
      throw new BusinessException(ErrorCode.DUPLICATE_PACKAGE_NAME);
    }
  }

  private void validateCommonFields(
      List<Long> coachIds,
      Integer totalHours,
      Integer durationMinutes,
      Integer validDays,
      BigDecimal originalPrice,
      BigDecimal price,
      Boolean refundEnabled,
      BigDecimal refundRatio,
      Integer refundValidDays) {
    if (CollectionUtils.isEmpty(coachIds)) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "适用教练至少选择 1 项");
    }
    if (coachIds.stream().anyMatch(id -> id == null || id <= 0)) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "适用教练 ID 非法");
    }
    if (totalHours == null || totalHours <= 0 || totalHours > MAX_HOURS) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "课时数需在 1-100 之间");
    }
    if (durationMinutes == null || durationMinutes <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "每节课时长需大于 0");
    }
    if (validDays == null || validDays <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "有效期需大于 0 天");
    }
    if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "原价不能为负数");
    }
    if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "售价不能为负数");
    }
    if (originalPrice.compareTo(price) < 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "原价不能小于售价");
    }
    if (refundEnabled != null && refundEnabled) {
      if (refundRatio == null || refundRatio.compareTo(BigDecimal.ZERO) < 0
          || refundRatio.compareTo(MAX_REFUND_RATIO) > 0) {
        throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "退款比例需在 0-100 之间");
      }
      if (refundValidDays == null || refundValidDays < 0) {
        throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "退款有效期不能为负数");
      }
    }
  }

  private void validateCoachIdsExist(List<Long> coachIds) {
    if (CollectionUtils.isEmpty(coachIds)) {
      return;
    }
    List<Long> distinctCoachIds = coachIds.stream().distinct().toList();
    Map<Long, Coach> coachMap = findCoachMap(Set.copyOf(distinctCoachIds));
    if (coachMap.size() != distinctCoachIds.size()) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "适用教练不存在");
    }
  }

  private void validateCustomConfig(AdminPackageTemplateCustomConfigRequest request) {
    if (request.minHours() == null || request.minHours() <= 0) {
      throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG, "最小课时需大于 0");
    }
    if (request.maxHours() == null || request.maxHours() <= 0 || request.maxHours() > MAX_HOURS) {
      throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG, "最大课时需在 1-100 之间");
    }
    if (request.minHours() > request.maxHours()) {
      throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG, "最小课时不能大于最大课时");
    }
    if (request.defaultValidDays() == null || request.defaultValidDays() <= 0) {
      throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG, "默认有效期需大于 0 天");
    }
  }

  private boolean nameExists(String name, Long excludeId) {
    LambdaQueryWrapper<PackageTemplate> wrapper = new LambdaQueryWrapper<PackageTemplate>()
        .eq(PackageTemplate::getName, name);
    if (excludeId != null) {
      wrapper.ne(PackageTemplate::getId, excludeId);
    }
    return packageTemplateMapper.selectCount(wrapper) > 0;
  }

  private String resolvePackageMode(String mode) {
    return PackageMode.isValid(mode) ? mode : DEFAULT_PACKAGE_MODE;
  }

  private String resolveTeachingType(String type) {
    return TeachingType.isValid(type) ? type : DEFAULT_TEACHING_TYPE;
  }

  private void saveCoachLinks(Long templateId, List<Long> coachIds) {
    packageTemplateCoachMapper.deleteByTemplateId(templateId);
    if (CollectionUtils.isEmpty(coachIds)) {
      return;
    }
    List<Long> distinctCoachIds = coachIds.stream().distinct().toList();
    Map<Long, Coach> coachMap = findCoachMap(Set.copyOf(distinctCoachIds));
    for (Long coachId : distinctCoachIds) {
      Coach coach = coachMap.get(coachId);
      if (coach == null) {
        throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "适用教练不存在");
      }
      PackageTemplateCoach link = new PackageTemplateCoach();
      link.setPackageTemplateId(templateId);
      link.setCoachId(coachId);
      link.setReferencePriceSnapshot(coach.getReferencePrice());
      packageTemplateCoachMapper.insert(link);
    }
  }

  private Map<Long, List<PackageTemplateCoach>> findCoachesByTemplateIds(Set<Long> templateIds) {
    if (CollectionUtils.isEmpty(templateIds)) {
      return Map.of();
    }
    LambdaQueryWrapper<PackageTemplateCoach> wrapper = new LambdaQueryWrapper<PackageTemplateCoach>()
        .in(PackageTemplateCoach::getPackageTemplateId, templateIds);
    List<PackageTemplateCoach> links = packageTemplateCoachMapper.selectList(wrapper);
    return links.stream().collect(Collectors.groupingBy(PackageTemplateCoach::getPackageTemplateId));
  }

  private List<Long> findTemplateIdsByCoachId(Long coachId) {
    LambdaQueryWrapper<PackageTemplateCoach> wrapper = new LambdaQueryWrapper<PackageTemplateCoach>()
        .eq(PackageTemplateCoach::getCoachId, coachId);
    return packageTemplateCoachMapper.selectList(wrapper).stream()
        .map(PackageTemplateCoach::getPackageTemplateId)
        .distinct()
        .toList();
  }

  private void validateTags(List<String> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return;
    }
    for (String tag : tags) {
      if (StringUtils.isBlank(tag)) {
        throw new BusinessException(ErrorCode.INVALID_TAG_FORMAT, "标签不能为空");
      }
      if (tag.length() > MAX_TAG_LENGTH) {
        throw new BusinessException(ErrorCode.INVALID_TAG_FORMAT, "标签长度不能超过 20 个字符");
      }
      if (!tag.matches(TAG_PATTERN)) {
        throw new BusinessException(ErrorCode.INVALID_TAG_FORMAT, "标签包含非法字符");
      }
    }
  }

  private Map<Long, Coach> findCoachMap(Set<Long> coachIds) {
    if (CollectionUtils.isEmpty(coachIds)) {
      return Map.of();
    }
    LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<Coach>()
        .in(Coach::getId, coachIds);
    return coachMapper.selectList(wrapper).stream()
        .collect(Collectors.toMap(Coach::getId, Function.identity(), (a, b) -> a));
  }

  private AdminPackageTemplateListItemResponse toListItem(
      PackageTemplate template,
      List<PackageTemplateCoach> links,
      Map<Long, Coach> coachMap) {
    List<Long> coachIds = links.stream().map(PackageTemplateCoach::getCoachId).toList();
    List<String> coachNames = coachIds.stream()
        .map(id -> {
          Coach coach = coachMap.get(id);
          return coach != null ? coach.getName() : null;
        })
        .filter(Objects::nonNull)
        .toList();
    return new AdminPackageTemplateListItemResponse(
        template.getId(),
        template.getName(),
        template.getPackageMode(),
        template.getTeachingType(),
        coachIds,
        coachNames,
        template.getTotalHours(),
        template.getDurationMinutes(),
        template.getValidDays(),
        template.getOriginalPrice(),
        template.getPrice(),
        template.getRefundEnabled(),
        template.getStatus(),
        template.getCreatedAt());
  }

  private AdminPackageTemplateDetailResponse toDetailResponse(PackageTemplate template) {
    List<PackageTemplateCoach> links = packageTemplateCoachMapper.findByTemplateId(template.getId());
    Set<Long> coachIds = links.stream().map(PackageTemplateCoach::getCoachId).collect(Collectors.toSet());
    Map<Long, Coach> coachMap = findCoachMap(coachIds);

    List<Long> coachIdList = links.stream().map(PackageTemplateCoach::getCoachId).toList();
    List<CoachBriefResponse> coachList = coachIdList.stream()
        .map(id -> {
          Coach coach = coachMap.get(id);
          return coach != null ? new CoachBriefResponse(coach.getId(), coach.getName(), coach.getAvatarUrl()) : null;
        })
        .filter(Objects::nonNull)
        .toList();

    return new AdminPackageTemplateDetailResponse(
        template.getId(),
        template.getName(),
        template.getPackageMode(),
        template.getTeachingType(),
        coachIdList,
        coachList,
        template.getStrokeIds(),
        template.getTotalHours(),
        template.getDurationMinutes(),
        template.getValidDays(),
        template.getOriginalPrice(),
        template.getPrice(),
        template.getRefundEnabled(),
        template.getRefundRatio(),
        template.getRefundValidDays(),
        template.getTags(),
        template.getDescription(),
        template.getImages() == null ? List.of() : template.getImages(),
        template.getStatus(),
        template.getCreatedAt(),
        template.getUpdatedAt(),
        template.getVersion());
  }
}
