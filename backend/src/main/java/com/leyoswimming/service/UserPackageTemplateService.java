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
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.exception.BusinessException;
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
  private static final List<Integer> ALLOWED_VALID_DAYS = List.of(30, 60, 90, 180);

  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageTemplateCoachMapper packageTemplateCoachMapper;
  private final CoachMapper coachMapper;
  private final CustomPackageConfigMapper customPackageConfigMapper;

  @Transactional(readOnly = true)
  public PackageListResponse list(PackageListRequest request) {
    LambdaQueryWrapper<PackageTemplate> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PackageTemplate::getStatus, PackageTemplateStatus.ACTIVE.getValue());
    wrapper.orderByDesc(PackageTemplate::getCreatedAt);

    Page<PackageTemplate> page = new Page<>(request.page(), request.pageSize());
    Page<PackageTemplate> result = packageTemplateMapper.selectPage(page, wrapper);

    List<PackageListItemResponse> items = result.getRecords().stream()
        .map(this::toListItem)
        .toList();

    return new PackageListResponse(
        items, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
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
    List<PackageDetailCoachResponse> applicableCoaches = coachIds.stream()
        .map(coachMap::get)
        .filter(Objects::nonNull)
        .filter(coach -> isPublicCoachStatus(coach.getStatus()))
        .map(this::toDetailCoach)
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

  private PackageDetailCoachResponse toDetailCoach(Coach coach) {
    return new PackageDetailCoachResponse(
        coach.getId(),
        coach.getName(),
        coach.getAvatarUrl(),
        coach.getRating(),
        coach.getTeachingYears(),
        coach.getTotalStudents(),
        coach.getReferencePrice(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getStatus());
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
