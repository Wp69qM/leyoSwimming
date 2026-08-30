package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachPackageListRequest;
import com.leyoswimming.dto.request.PackageDetailRequest;
import com.leyoswimming.dto.request.PackageListRequest;
import com.leyoswimming.dto.response.CoachPackageListResponse;
import com.leyoswimming.dto.response.PackageDetailCoachResponse;
import com.leyoswimming.dto.response.PackageDetailResponse;
import com.leyoswimming.dto.response.PackageListItemResponse;
import com.leyoswimming.dto.response.PackageListResponse;
import com.leyoswimming.dto.response.UserPackageTemplateCustomConfigResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class UserPackageTemplateService {

  private static final int DEFAULT_CUSTOM_HOURS_MIN = 1;
  private static final int DEFAULT_CUSTOM_HOURS_MAX = 50;
  private static final int DEFAULT_CUSTOM_VALID_DAYS = 30;
  private static final int CUSTOM_PACKAGE_TOTAL_HOURS = 0;
  private static final int CUSTOM_PACKAGE_DURATION_MINUTES = 60;
  private static final List<Integer> ALLOWED_VALID_DAYS = List.of(30, 60, 90, 180);
  private static final String CUSTOM_PACKAGE_NAME = "自定义课时";
  private static final String CUSTOM_PACKAGE_TEACHING_TYPE = "one_on_one";
  private static final Long CUSTOM_PACKAGE_SYNTHETIC_ID = -1L;

  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageTemplateCoachMapper packageTemplateCoachMapper;
  private final CoachMapper coachMapper;
  private final CoachCertificateMapper coachCertificateMapper;
  private final CustomPackageConfigMapper customPackageConfigMapper;

  @Transactional(readOnly = true)
  public PackageListResponse list(PackageListRequest request) {
    LambdaQueryWrapper<PackageTemplate> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PackageTemplate::getStatus, PackageTemplateStatus.ACTIVE.getValue());
    wrapper.orderByDesc(PackageTemplate::getCreatedAt);

    Optional<PackageListItemResponse> customItem = buildCustomPackageListItem();
    int pageSize = Math.max(1, request.pageSize() - (customItem.isPresent() ? 1 : 0));

    Page<PackageTemplate> page = new Page<>(request.page(), pageSize);
    Page<PackageTemplate> result = packageTemplateMapper.selectPage(page, wrapper);

    List<PackageListItemResponse> items = new ArrayList<>(result.getRecords().stream()
        .map(this::toListItem)
        .toList());

    boolean customAdded = customItem.isPresent() && items.size() < request.pageSize()
        && items.add(customItem.orElseThrow());
    long total = result.getTotal() + (customItem.isPresent() ? 1 : 0);

    return new PackageListResponse(
        items, total, (int) result.getCurrent(), (int) result.getSize());
  }

  @Transactional(readOnly = true)
  public CoachPackageListResponse coachPackages(CoachPackageListRequest request) {
    Coach coach = coachMapper.selectById(request.coachId());
    if (coach == null || !isPublicCoachStatus(coach.getStatus())) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }

    LambdaQueryWrapper<PackageTemplateCoach> linkWrapper = new LambdaQueryWrapper<PackageTemplateCoach>()
        .eq(PackageTemplateCoach::getCoachId, coach.getId());
    List<PackageTemplateCoach> links = packageTemplateCoachMapper.selectList(linkWrapper);
    List<Long> templateIds = links.stream()
        .map(PackageTemplateCoach::getPackageTemplateId)
        .distinct()
        .toList();

    List<PackageTemplate> templates;
    if (CollectionUtils.isEmpty(templateIds)) {
      templates = Collections.emptyList();
    } else {
      LambdaQueryWrapper<PackageTemplate> wrapper = new LambdaQueryWrapper<PackageTemplate>()
          .in(PackageTemplate::getId, templateIds)
          .eq(PackageTemplate::getStatus, PackageTemplateStatus.ACTIVE.getValue())
          .orderByAsc(PackageTemplate::getTotalHours);
      templates = packageTemplateMapper.selectList(wrapper);
    }

    List<PackageListItemResponse> standardPackages = templates.stream()
        .map(this::toListItem)
        .toList();

    boolean customEnabled = coach.getReferencePrice() != null
        && coach.getReferencePrice().compareTo(BigDecimal.ZERO) > 0;
    CustomPackageConfig config = customPackageConfigMapper.findFirst();
    int customMin = config != null && config.getMinHours() != null
        ? config.getMinHours() : DEFAULT_CUSTOM_HOURS_MIN;
    int customMax = config != null && config.getMaxHours() != null
        ? config.getMaxHours() : DEFAULT_CUSTOM_HOURS_MAX;

    return new CoachPackageListResponse(
        coach.getId(), coach.getStatus(), coach.getReferencePrice(),
        standardPackages, customEnabled, customMin, customMax);
  }

  @Transactional(readOnly = true)
  public PackageDetailResponse detail(PackageDetailRequest request) {
    Optional<PackageDetailResponse> customDetail = buildCustomPackageDetail(
        request.packageId(), request.coachId());
    if (customDetail.isPresent()) {
      return customDetail.get();
    }

    PackageTemplate template = packageTemplateMapper.selectById(request.packageId());
    if (template == null
        || !PackageTemplateStatus.ACTIVE.getValue().equals(template.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }

    List<PackageTemplateCoach> links = packageTemplateCoachMapper.findByTemplateId(template.getId());
    Set<Long> coachIds = links.stream()
        .map(PackageTemplateCoach::getCoachId)
        .collect(Collectors.toSet());

    Long selectedCoachId = request.coachId();
    if (selectedCoachId != null) {
      Coach selectedCoach = coachMapper.selectById(selectedCoachId);
      if (selectedCoach == null || !isPublicCoachStatus(selectedCoach.getStatus())) {
        throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
      }
      if (!coachIds.contains(selectedCoachId)) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
      }
      coachIds = Set.of(selectedCoachId);
    }

    Map<Long, Coach> coachMap = findCoachMap(coachIds);
    Map<Long, String> avatarMap = resolveAvatars(coachIds);
    List<PackageDetailCoachResponse> applicableCoaches = coachIds.stream()
        .map(coachMap::get)
        .filter(Objects::nonNull)
        .filter(coach -> isPublicCoachStatus(coach.getStatus()))
        .map(coach -> toDetailCoach(coach, avatarMap.get(coach.getId())))
        .toList();

    return new PackageDetailResponse(
        template.getId(),
        template.getName(),
        template.getPackageMode(),
        template.getTeachingType(),
        template.getTotalHours(),
        template.getDurationMinutes(),
        template.getValidDays(),
        template.getOriginalPrice(),
        template.getPrice(),
        template.getRefundEnabled(),
        template.getRefundRatio(),
        template.getRefundValidDays(),
        buildRefundPolicySummary(template),
        template.getTags(),
        template.getDescription(),
        template.getImages() == null ? List.of() : template.getImages(),
        applicableCoaches);
  }

  @Transactional(readOnly = true)
  public UserPackageTemplateCustomConfigResponse customConfig() {
    CustomPackageConfig config = customPackageConfigMapper.findFirst();
    Integer minHours = config != null && config.getMinHours() != null
        ? config.getMinHours() : DEFAULT_CUSTOM_HOURS_MIN;
    Integer maxHours = config != null && config.getMaxHours() != null
        ? config.getMaxHours() : DEFAULT_CUSTOM_HOURS_MAX;
    Integer defaultValidDays = config != null && config.getDefaultValidDays() != null
        ? config.getDefaultValidDays() : DEFAULT_CUSTOM_VALID_DAYS;
    return new UserPackageTemplateCustomConfigResponse(
        minHours, maxHours, defaultValidDays, ALLOWED_VALID_DAYS);
  }

  /**
   * 构建合成的自定义套餐列表项。当全局自定义套餐配置存在且至少有一位公开教练设置参考单价时返回。
   * 使用固定的 {@link #CUSTOM_PACKAGE_SYNTHETIC_ID} 作为 synthetic package id，避免与标准模板 id 冲突。
   */
  private Optional<PackageListItemResponse> buildCustomPackageListItem() {
    CustomPackageConfig config = customPackageConfigMapper.findFirst();
    if (config == null || config.getId() == null) {
      return Optional.empty();
    }
    List<Coach> coaches = findCoachesWithReferencePrice();
    if (coaches.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new PackageListItemResponse(
        CUSTOM_PACKAGE_SYNTHETIC_ID,
        CUSTOM_PACKAGE_NAME,
        PackageMode.CUSTOM.getValue(),
        CUSTOM_PACKAGE_TEACHING_TYPE,
        CUSTOM_PACKAGE_TOTAL_HOURS,
        CUSTOM_PACKAGE_DURATION_MINUTES,
        config.getDefaultValidDays() != null
            ? config.getDefaultValidDays() : DEFAULT_CUSTOM_VALID_DAYS,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        List.of(),
        null,
        buildCustomRefundPolicySummary()));
  }

  /**
   * 构建合成的自定义套餐详情。当请求 packageId 为合成自定义套餐 ID 时返回。
   */
  private Optional<PackageDetailResponse> buildCustomPackageDetail(
      Long packageId, Long selectedCoachId) {
    if (!CUSTOM_PACKAGE_SYNTHETIC_ID.equals(packageId)) {
      return Optional.empty();
    }

    CustomPackageConfig config = customPackageConfigMapper.findFirst();
    if (config == null || config.getId() == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }

    List<Coach> coaches = findCoachesWithReferencePrice();
    if (coaches.isEmpty()) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }

    Set<Long> coachIds = coaches.stream()
        .map(Coach::getId)
        .collect(Collectors.toSet());

    if (selectedCoachId != null) {
      Coach selectedCoach = coachMapper.selectById(selectedCoachId);
      if (selectedCoach == null || !isPublicCoachStatus(selectedCoach.getStatus())) {
        throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
      }
      if (!coachIds.contains(selectedCoachId)) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
      }
      coachIds = Set.of(selectedCoachId);
    }

    Set<Long> coachIdSet = coachIds;
    Map<Long, String> avatarMap = resolveAvatars(coachIdSet);
    List<PackageDetailCoachResponse> applicableCoaches = coachIds.stream()
        .map(id -> coaches.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null))
        .filter(Objects::nonNull)
        .map(coach -> toDetailCoach(coach, avatarMap.get(coach.getId())))
        .toList();

    return Optional.of(new PackageDetailResponse(
        CUSTOM_PACKAGE_SYNTHETIC_ID,
        CUSTOM_PACKAGE_NAME,
        PackageMode.CUSTOM.getValue(),
        CUSTOM_PACKAGE_TEACHING_TYPE,
        CUSTOM_PACKAGE_TOTAL_HOURS,
        CUSTOM_PACKAGE_DURATION_MINUTES,
        config.getDefaultValidDays() != null
            ? config.getDefaultValidDays() : DEFAULT_CUSTOM_VALID_DAYS,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        true,
        BigDecimal.ONE,
        0,
        buildCustomRefundPolicySummary(),
        List.of(),
        null,
        List.of(),
        applicableCoaches));
  }

  private List<Coach> findCoachesWithReferencePrice() {
    LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<Coach>()
        .gt(Coach::getReferencePrice, BigDecimal.ZERO)
        .in(Coach::getStatus,
            CoachStatus.APPROVED.getValue(), CoachStatus.RESIGNING.getValue());
    return coachMapper.selectList(wrapper);
  }

  private String buildCustomRefundPolicySummary() {
    return "按实际购买课时退款，详见购买须知";
  }

  /**
   * 判定教练是否处于用户端可见状态。
   * 与 {@link UserCoachService#PUBLIC_STATUSES} 保持一致：已通过和离职交接中的教练仍可在用户端展示，
   * 实际能否预约由套餐/排期层面的冻结逻辑控制。
   */
  private boolean isPublicCoachStatus(Integer status) {
    return status != null
        && (status == CoachStatus.APPROVED.getValue()
            || status == CoachStatus.RESIGNING.getValue());
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

  private PackageListItemResponse toListItem(PackageTemplate template) {
    String imageUrl = CollectionUtils.isEmpty(template.getImages()) ? null : template.getImages().get(0);
    return new PackageListItemResponse(
        template.getId(),
        template.getName(),
        template.getPackageMode(),
        template.getTeachingType(),
        template.getTotalHours(),
        template.getDurationMinutes(),
        template.getValidDays(),
        template.getOriginalPrice(),
        template.getPrice(),
        template.getTags(),
        imageUrl,
        buildRefundPolicySummary(template));
  }

  private PackageDetailCoachResponse toDetailCoach(Coach coach, String avatarUrl) {
    return new PackageDetailCoachResponse(
        coach.getId(),
        coach.getName(),
        avatarUrl,
        coach.getRating(),
        coach.getTeachingYears(),
        coach.getTotalStudents(),
        coach.getReferencePrice(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getStatus());
  }

  private Map<Long, String> resolveAvatars(Set<Long> coachIds) {
    if (CollectionUtils.isEmpty(coachIds)) {
      return Map.of();
    }
    List<CoachCertificate> certificates = coachCertificateMapper.findByCoachIds(new ArrayList<>(coachIds));
    if (certificates == null) {
      return Map.of();
    }
    return certificates.stream()
        .filter(cert -> "PORTRAIT".equalsIgnoreCase(cert.getCertType()))
        .collect(Collectors.toMap(
            CoachCertificate::getCoachId,
            CoachCertificate::getImageUrl,
            (a, b) -> a));
  }

  private List<String> parseTeachingStrokes(String strokes) {
    if (StringUtils.isBlank(strokes)) {
      return List.of();
    }
    String[] parts = strokes.split(",");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private String buildRefundPolicySummary(PackageTemplate template) {
    if (Boolean.TRUE.equals(template.getRefundEnabled())) {
      return String.format(
          "开课后 %d 天内可申请退款，退款比例 %s%%",
          template.getRefundValidDays() == null ? 0 : template.getRefundValidDays(),
          template.getRefundRatio() == null ? "0" : template.getRefundRatio().toPlainString());
    }
    return "本套餐购买后不可退款";
  }
}
