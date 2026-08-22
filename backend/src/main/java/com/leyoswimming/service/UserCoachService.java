package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachDetailRequest;
import com.leyoswimming.dto.request.CoachListRequest;
import com.leyoswimming.dto.response.CoachDetailAvailableTimeResponse;
import com.leyoswimming.dto.response.CoachDetailCertificateResponse;
import com.leyoswimming.dto.response.CoachDetailContactResponse;
import com.leyoswimming.dto.response.CoachDetailPackageResponse;
import com.leyoswimming.dto.response.CoachDetailResponse;
import com.leyoswimming.dto.response.CoachDetailReviewResponse;
import com.leyoswimming.dto.response.CoachListItemResponse;
import com.leyoswimming.dto.response.CoachListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachCertificate;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachCertificateMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.util.PhoneEncryptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCoachService {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 10;
  private static final int MAX_PREVIEW_PACKAGES = 3;

  private static final List<Integer> PUBLIC_STATUSES =
      List.of(CoachStatus.APPROVED.getValue(), CoachStatus.RESIGNING.getValue());

  private final CoachMapper coachMapper;
  private final CoachCertificateMapper certificateMapper;
  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageTemplateCoachMapper packageTemplateCoachMapper;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional(readOnly = true)
  public CoachListResponse list(CoachListRequest request) {
    int page = request.page() == null ? DEFAULT_PAGE : request.page();
    int pageSize = request.pageSize() == null ? DEFAULT_PAGE_SIZE : request.pageSize();
    String keyword = request.keyword();
    String sortBy = request.sortBy();
    String sortOrder = request.sortOrder();

    LambdaQueryWrapper<Coach> wrapper =
        new LambdaQueryWrapper<Coach>().in(Coach::getStatus, PUBLIC_STATUSES);

    if (StringUtils.isNotBlank(keyword)) {
      wrapper.like(Coach::getName, keyword.trim());
    }

    applyListSort(wrapper, sortBy, sortOrder);
    wrapper.orderByAsc(Coach::getId);

    Page<Coach> pageResult = coachMapper.selectPage(new Page<>(page, pageSize), wrapper);

    List<CoachListItemResponse> items =
        pageResult.getRecords().stream().map(this::toListItem).toList();
    return new CoachListResponse(
        items, pageResult.getTotal(), page, pageSize);
  }

  private void applyListSort(LambdaQueryWrapper<Coach> wrapper, String sortBy, String sortOrder) {
    boolean desc = sortOrder == null || "desc".equalsIgnoreCase(sortOrder);
    String effectiveSortBy = sortBy == null ? "rating" : sortBy;

    switch (effectiveSortBy) {
      case "price" -> {
        if (desc) {
          wrapper.orderByDesc(Coach::getReferencePrice);
        } else {
          wrapper.orderByAsc(Coach::getReferencePrice);
        }
      }
      case "time" -> {
        // MVP 阶段可约时段表尚未启用，按综合评分降序作为兜底
        wrapper.orderByDesc(Coach::getRating);
      }
      default -> {
        if (desc) {
          wrapper.orderByDesc(Coach::getRating);
        } else {
          wrapper.orderByAsc(Coach::getRating);
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public CoachDetailResponse detail(CoachDetailRequest request) {
    Coach coach = findPublicCoach(request.coachId());
    return toDetailResponse(coach);
  }

  private Coach findPublicCoach(Long coachId) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null || !PUBLIC_STATUSES.contains(coach.getStatus())) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    return coach;
  }

  private CoachListItemResponse toListItem(Coach coach) {
    return new CoachListItemResponse(
        coach.getId(),
        coach.getName(),
        coach.getStatus(),
        coach.getAvatarUrl(),
        coach.getRating(),
        coach.getTeachingYears(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getRealtimeStatus());
  }

  private CoachDetailResponse toDetailResponse(Coach coach) {
    return new CoachDetailResponse(
        coach.getId(),
        coach.getName(),
        coach.getAvatarUrl(),
        parseGender(coach.getGender()),
        coach.getAge(),
        coach.getStatus(),
        coach.getRating(),
        coach.getTeachingYears(),
        coach.getTotalStudents(),
        coach.getTotalHours(),
        parseTeachingStrokes(coach.getTeachingStrokes()),
        coach.getBio(),
        coach.getReferencePrice(),
        buildContact(coach),
        buildCertificates(coach.getId()),
        buildPreviewPackages(coach.getId()),
        buildReviews(),
        buildAvailableTimes(),
        coach.getRealtimeStatus());
  }

  private Integer parseGender(String gender) {
    if (StringUtils.isBlank(gender)) {
      return null;
    }
    return switch (gender.trim()) {
      case "男" -> 1;
      case "女" -> 2;
      default -> null;
    };
  }

  private List<String> parseTeachingStrokes(String strokes) {
    if (StringUtils.isBlank(strokes)) {
      return List.of();
    }
    return Arrays.stream(strokes.split(","))
        .filter(StringUtils::isNotBlank)
        .map(String::trim)
        .distinct()
        .toList();
  }

  private CoachDetailContactResponse buildContact(Coach coach) {
    String phone = decryptPhone(coach.getPhone());
    return new CoachDetailContactResponse(
        PhoneEncryptor.mask(phone), coach.getWechatQrUrl());
  }

  private String decryptPhone(String encryptedPhone) {
    if (StringUtils.isBlank(encryptedPhone)) {
      return null;
    }
    try {
      return phoneEncryptor.decrypt(encryptedPhone);
    } catch (Exception e) {
      log.warn("解密教练手机号失败", e);
      return null;
    }
  }

  private List<CoachDetailCertificateResponse> buildCertificates(Long coachId) {
    List<CoachCertificate> entities = certificateMapper.findByCoachId(coachId);
    return entities.stream()
        .filter(cert -> !"PORTRAIT".equalsIgnoreCase(cert.getCertType()))
        .map(cert -> new CoachDetailCertificateResponse(cert.getCertType(), cert.getImageUrl()))
        .toList();
  }

  private List<CoachDetailPackageResponse> buildPreviewPackages(Long coachId) {
    LambdaQueryWrapper<PackageTemplateCoach> linkWrapper =
        new LambdaQueryWrapper<PackageTemplateCoach>()
            .eq(PackageTemplateCoach::getCoachId, coachId);
    List<Long> templateIds =
        packageTemplateCoachMapper.selectList(linkWrapper).stream()
            .map(PackageTemplateCoach::getPackageTemplateId)
            .distinct()
            .toList();

    if (templateIds.isEmpty()) {
      return List.of();
    }

    LambdaQueryWrapper<PackageTemplate> templateWrapper =
        new LambdaQueryWrapper<PackageTemplate>()
            .in(PackageTemplate::getId, templateIds)
            .eq(PackageTemplate::getStatus, PackageTemplateStatus.ACTIVE.getValue())
            .orderByDesc(PackageTemplate::getPrice);
    List<PackageTemplate> templates = packageTemplateMapper.selectList(templateWrapper);

    List<CoachDetailPackageResponse> experienceFirst = new ArrayList<>();
    List<CoachDetailPackageResponse> standardList = new ArrayList<>();
    for (PackageTemplate template : templates) {
      CoachDetailPackageResponse item = toPackageResponse(template);
      if (PackageMode.EXPERIENCE.getValue().equals(template.getPackageMode())) {
        experienceFirst.add(item);
      } else {
        standardList.add(item);
      }
    }

    experienceFirst.addAll(standardList);
    return experienceFirst.stream().limit(MAX_PREVIEW_PACKAGES).toList();
  }

  private CoachDetailPackageResponse toPackageResponse(PackageTemplate template) {
    String imageUrl = org.springframework.util.CollectionUtils.isEmpty(template.getImages())
        ? null : template.getImages().get(0);
    return new CoachDetailPackageResponse(
        template.getId(),
        template.getName(),
        template.getPackageMode(),
        template.getPrice(),
        template.getTotalHours(),
        imageUrl);
  }

  private List<CoachDetailReviewResponse> buildReviews() {
    // MVP 阶段教练评价由后续 US 补充，当前返回空列表
    return List.of();
  }

  private List<CoachDetailAvailableTimeResponse> buildAvailableTimes() {
    // MVP 阶段可约时间预览由 US-016 自动释放时段后补充，当前返回空列表
    return List.of();
  }
}
