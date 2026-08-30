package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.dto.ai.internal.InternalCoachItemResponse;
import com.leyoswimming.dto.ai.internal.InternalHotRecommendationsRequest;
import com.leyoswimming.dto.ai.internal.InternalPackageItemResponse;
import com.leyoswimming.dto.ai.internal.InternalPackageQueryRequest;
import com.leyoswimming.dto.ai.internal.InternalUserPackageItemResponse;
import com.leyoswimming.dto.ai.internal.InternalUserPackagesRequest;
import com.leyoswimming.dto.ai.internal.InternalUserProfileRequest;
import com.leyoswimming.dto.ai.internal.InternalUserProfileResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.enums.SwimStroke;
import com.leyoswimming.enums.TeachingType;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.AiUserHashUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalAiDataService {

  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 20;
  private static final List<Integer> PUBLIC_COACH_STATUSES =
      List.of(CoachStatus.APPROVED.getValue(), CoachStatus.RESIGNING.getValue());

  private final CoachMapper coachMapper;
  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageMapper packageMapper;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public List<InternalCoachItemResponse> queryCoaches(
      String strokeCode, String genderCode, Integer minPrice, Integer maxPrice,
      Integer maxAge, String classSize, Integer limit) {
    int effectiveLimit = effectiveLimit(limit);
    String strokeLabel = StringUtils.isBlank(strokeCode) ? null : SwimStroke.toLabel(strokeCode);
    String genderLabel = normalizeGender(genderCode);

    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>().in(Coach::getStatus, PUBLIC_COACH_STATUSES);

    if (StringUtils.isNotBlank(genderLabel)) {
      // 数据库中可能存中文（男/女）也可能存英文（male/female），同时匹配两种写法
      String altGenderLabel =
          "女".equals(genderLabel) ? "female" : ("男".equals(genderLabel) ? "male" : null);
      if (StringUtils.isNotBlank(altGenderLabel)) {
        wrapper.and(
            w ->
                w.eq(Coach::getGender, genderLabel)
                    .or()
                    .eq(Coach::getGender, altGenderLabel));
      } else {
        wrapper.eq(Coach::getGender, genderLabel);
      }
    }
    if (minPrice != null) {
      wrapper.ge(Coach::getReferencePrice, BigDecimal.valueOf(minPrice));
    }
    if (maxPrice != null) {
      wrapper.le(Coach::getReferencePrice, BigDecimal.valueOf(maxPrice));
    }
    if (maxAge != null) {
      wrapper.le(Coach::getAge, maxAge);
    }
    // MVP 阶段教练不维护班级规模，classSize 过滤暂不生效

    List<Coach> coaches = coachMapper.selectList(wrapper);
    Stream<Coach> stream = coaches.stream();
    if (StringUtils.isNotBlank(strokeLabel)) {
      stream = stream.filter(coach -> matchesStroke(coach.getTeachingStrokes(), strokeLabel));
    }

    return stream.limit(effectiveLimit).map(this::toCoachItem).toList();
  }

  @Transactional(readOnly = true)
  public List<InternalPackageItemResponse> queryPackages(
      String strokeCode, String packageMode, Integer minPrice, Integer maxPrice,
      Integer hours, Integer limit) {
    int effectiveLimit = effectiveLimit(limit);

    if (PackageMode.CUSTOM.getValue().equalsIgnoreCase(packageMode)) {
      return queryCustomPackages(strokeCode, minPrice, maxPrice, effectiveLimit);
    }

    LambdaQueryWrapper<PackageTemplate> wrapper =
        new LambdaQueryWrapper<PackageTemplate>()
            .eq(PackageTemplate::getStatus, PackageTemplateStatus.ACTIVE.getValue());

    if (PackageMode.isValid(packageMode)) {
      wrapper.eq(PackageTemplate::getPackageMode, packageMode);
    }
    if (hours != null) {
      wrapper.eq(PackageTemplate::getTotalHours, hours);
    }
    if (minPrice != null) {
      wrapper.ge(PackageTemplate::getPrice, BigDecimal.valueOf(minPrice));
    }
    if (maxPrice != null) {
      wrapper.le(PackageTemplate::getPrice, BigDecimal.valueOf(maxPrice));
    }

    List<PackageTemplate> templates = packageTemplateMapper.selectList(wrapper);
    Stream<PackageTemplate> stream = templates.stream();
    if (StringUtils.isNotBlank(strokeCode)) {
      stream = stream.filter(template -> matchesStrokeId(template.getStrokeIds(), strokeCode));
    }

    return stream.limit(effectiveLimit).map(this::toPackageItem).toList();
  }

  private List<InternalPackageItemResponse> queryCustomPackages(
      String strokeCode, Integer minPrice, Integer maxPrice, int limit) {
    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>().in(Coach::getStatus, PUBLIC_COACH_STATUSES);
    if (minPrice != null) {
      wrapper.ge(Coach::getReferencePrice, BigDecimal.valueOf(minPrice));
    }
    if (maxPrice != null) {
      wrapper.le(Coach::getReferencePrice, BigDecimal.valueOf(maxPrice));
    }

    List<Coach> coaches = coachMapper.selectList(wrapper);
    Stream<Coach> stream = coaches.stream();
    if (StringUtils.isNotBlank(strokeCode)) {
      String strokeLabel = SwimStroke.toLabel(strokeCode);
      stream = stream.filter(coach -> matchesStroke(coach.getTeachingStrokes(), strokeLabel));
    }

    return stream.limit(limit).map(this::toCustomPackageItem).toList();
  }

  @Transactional(readOnly = true)
  public InternalUserProfileResponse queryUserProfile(InternalUserProfileRequest request) {
    User user = userMapper.selectById(request.userId());
    if (user == null) {
      return null;
    }

    String targetStroke = null;
    if (user.getSwimStrokes() != null && !user.getSwimStrokes().isEmpty()) {
      targetStroke = SwimStroke.toCode(user.getSwimStrokes().get(0));
    }

    return new InternalUserProfileResponse(
        user.getId(),
        AiUserHashUtil.hash(user.getId()),
        user.getAge(),
        targetStroke,
        resolveSwimmingLevel(user),
        null,
        Boolean.TRUE.equals(isMinor(user)));
  }

  @Transactional(readOnly = true)
  public List<InternalUserPackageItemResponse> queryUserPackages(
      InternalUserPackagesRequest request) {
    LambdaQueryWrapper<CoursePackage> wrapper =
        new LambdaQueryWrapper<CoursePackage>().eq(CoursePackage::getUserId, request.userId());
    if (request.statuses() != null && !request.statuses().isEmpty()) {
      wrapper.in(CoursePackage::getStatus, request.statuses());
    }
    List<CoursePackage> packages = packageMapper.selectList(wrapper);
    return packages.stream().map(this::toUserPackageItem).toList();
  }

  @Transactional(readOnly = true)
  public List<Object> queryHotRecommendations(InternalHotRecommendationsRequest request) {
    List<Object> results = new ArrayList<>();
    results.addAll(
        queryCoaches(request.stroke(), null, null, null, null, null, request.limit()));
    results.addAll(
        queryPackages(request.stroke(), null, null, null, null, request.limit()));
    return results.stream().limit(effectiveLimit(request.limit())).toList();
  }

  private InternalCoachItemResponse toCoachItem(Coach coach) {
    return new InternalCoachItemResponse(
        AiUserHashUtil.coachHash(coach.getId()),
        coach.getId(),
        coach.getName(),
        coach.getAvatarUrl(),
        normalizeGenderCode(coach.getGender()),
        coach.getAge(),
        coach.getRating() == null ? null : coach.getRating().doubleValue(),
        coach.getReferencePrice() == null ? null : coach.getReferencePrice().intValue(),
        coach.getTeachingYears(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getBio());
  }

  private InternalPackageItemResponse toPackageItem(PackageTemplate template) {
    return new InternalPackageItemResponse(
        AiUserHashUtil.packageHash(template.getId()),
        template.getId(),
        template.getPackageMode(),
        template.getName(),
        template.getTotalHours(),
        template.getPrice() == null ? null : template.getPrice().intValue(),
        pricePerHour(template.getPrice(), template.getTotalHours()),
        template.getValidDays(),
        TeachingType.fromValue(template.getTeachingType()) != null
            ? TeachingType.fromValue(template.getTeachingType()).getLabel()
            : template.getTeachingType(),
        mapStrokeIds(template.getStrokeIds()),
        null,
        null,
        null,
        template.getDescription());
  }

  private InternalPackageItemResponse toCustomPackageItem(Coach coach) {
    return new InternalPackageItemResponse(
        null,
        null,
        PackageMode.CUSTOM.getValue(),
        coach.getName(),
        null,
        null,
        null,
        null,
        TeachingType.ONE_ON_ONE.getLabel(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getId(),
        coach.getName(),
        coach.getReferencePrice() == null ? null : coach.getReferencePrice().intValue(),
        coach.getBio());
  }

  private InternalUserPackageItemResponse toUserPackageItem(CoursePackage pkg) {
    return new InternalUserPackageItemResponse(
        AiUserHashUtil.packageHash(pkg.getId()),
        pkg.getId(),
        pkg.getPackageName(),
        pkg.getStatus(),
        pkg.getTotalHours(),
        pkg.getAvailableCount(),
        mapStrokeIds(pkg.getStrokeIds()));
  }

  private boolean matchesStroke(String teachingStrokes, String strokeLabel) {
    if (StringUtils.isBlank(teachingStrokes) || StringUtils.isBlank(strokeLabel)) {
      return false;
    }
    return Arrays.stream(teachingStrokes.split(","))
        .map(String::trim)
        .anyMatch(strokeLabel::equals);
  }

  private boolean matchesStrokeId(List<Integer> strokeIds, String strokeCode) {
    if (strokeIds == null || strokeIds.isEmpty() || StringUtils.isBlank(strokeCode)) {
      return false;
    }
    String strokeLabel = SwimStroke.toLabel(strokeCode);
    return strokeIds.stream()
        .map(SwimStroke::labelFromId)
        .filter(Objects::nonNull)
        .anyMatch(strokeLabel::equals);
  }

  private List<String> parseTeachingStrokes(String strokes) {
    if (StringUtils.isBlank(strokes)) {
      return List.of();
    }
    return Arrays.stream(strokes.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .toList();
  }

  private List<String> mapStrokeIds(List<Integer> strokeIds) {
    if (strokeIds == null || strokeIds.isEmpty()) {
      return List.of();
    }
    return strokeIds.stream()
        .map(SwimStroke::codeFromId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private Integer pricePerHour(BigDecimal price, Integer hours) {
    if (price == null || hours == null || hours == 0) {
      return null;
    }
    return price.divide(BigDecimal.valueOf(hours), 0, java.math.RoundingMode.HALF_UP).intValue();
  }

  private String normalizeGender(String genderCode) {
    if (StringUtils.isBlank(genderCode)) {
      return null;
    }
    return switch (genderCode.toLowerCase()) {
      case "female", "f", "女" -> "女";
      case "male", "m", "男" -> "男";
      default -> null;
    };
  }

  private String normalizeGenderCode(String gender) {
    if (StringUtils.isBlank(gender)) {
      return null;
    }
    return switch (gender.toLowerCase()) {
      case "女", "female", "f" -> "female";
      case "男", "male", "m" -> "male";
      default -> null;
    };
  }

  private String resolveSwimmingLevel(User user) {
    if (!Boolean.TRUE.equals(user.getHasSwimBasis())) {
      return "beginner";
    }
    if (user.getSwimYears() != null && user.getSwimYears() >= 3) {
      return "advanced";
    }
    return "intermediate";
  }

  private Boolean isMinor(User user) {
    // MVP 阶段用户表未单独设置 minor 字段，以 identity 字段兜底：0 未知/1 成人/2 未成年人
    if (user.getIdentity() == null) {
      return false;
    }
    return user.getIdentity() == 2;
  }

  private int effectiveLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }
}
