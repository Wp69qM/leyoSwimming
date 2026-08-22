package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.CoachPackageDetailRequest;
import com.leyoswimming.dto.request.CoachStudentDetailRequest;
import com.leyoswimming.dto.request.CoachStudentListRequest;
import com.leyoswimming.dto.request.CoachStudentPackageListRequest;
import com.leyoswimming.dto.request.CoachStudentUpdateRequest;
import com.leyoswimming.dto.response.CoachPackageDetailResponse;
import com.leyoswimming.dto.response.CoachStudentDetailResponse;
import com.leyoswimming.dto.response.CoachStudentListResponse;
import com.leyoswimming.dto.response.CoachStudentPackageListResponse;
import com.leyoswimming.entity.Booking;
import com.leyoswimming.entity.CoachStudentProfile;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.ActorType;
import com.leyoswimming.enums.SwimStroke;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.BookingMapper;
import com.leyoswimming.repository.CoachStudentProfileMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.HtmlUtils;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachStudentService {

  private static final String PACKAGE_MODE_EXPERIENCE = "experience";
  private static final String PACKAGE_MODE_STANDARD = "standard";
  private static final String PACKAGE_STATUS_ACTIVE = "active";
  private static final String TAB_ACTIVE = "active";
  private static final String TAB_HISTORY = "history";

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final CoachStudentProfileMapper coachStudentProfileMapper;
  private final PackageMapper packageMapper;
  private final UserMapper userMapper;
  private final BookingMapper bookingMapper;
  private final IdempotencyHelper idempotencyHelper;

  @Transactional(readOnly = true)
  public CoachStudentListResponse list(Long coachId, CoachStudentListRequest request) {
    if (!TAB_ACTIVE.equals(request.tab()) && !TAB_HISTORY.equals(request.tab())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "tab 参数错误");
    }

    List<CoursePackage> coachPackages = getCoachPackages(coachId);
    Set<Long> activeStudentIds = new HashSet<>();
    Set<Long> historyStudentIds = new HashSet<>();
    Map<Long, List<CoursePackage>> studentPackages = new HashMap<>();

    for (CoursePackage pkg : coachPackages) {
      Long userId = pkg.getUserId();
      if (userId == null) {
        continue;
      }
      studentPackages.computeIfAbsent(userId, k -> new ArrayList<>()).add(pkg);
      if (PACKAGE_STATUS_ACTIVE.equals(pkg.getStatus())) {
        activeStudentIds.add(userId);
      } else {
        historyStudentIds.add(userId);
      }
    }

    Set<Long> targetStudentIds;
    if (TAB_ACTIVE.equals(request.tab())) {
      targetStudentIds = new HashSet<>(activeStudentIds);
    } else {
      targetStudentIds = new HashSet<>(historyStudentIds);
      targetStudentIds.removeAll(activeStudentIds);
    }

    if (targetStudentIds.isEmpty()) {
      return new CoachStudentListResponse(List.of());
    }

    List<User> users = userMapper.selectBatchIds(targetStudentIds.stream().toList());
    List<CoachStudentListResponse.StudentItem> items = new ArrayList<>();

    for (User user : users) {
      if (user == null) {
        continue;
      }
      if (StringUtils.isNotBlank(request.keyword())
          && (user.getName() == null
              || !user.getName().contains(request.keyword().trim()))) {
        continue;
      }
      List<CoursePackage> packages = studentPackages.getOrDefault(user.getId(), List.of());
      List<CoachStudentListResponse.PackageTag> tags = buildActivePackageTags(packages);
      items.add(
          new CoachStudentListResponse.StudentItem(
              user.getId(),
              user.getAvatarUrl(),
              maskName(user),
              user.getGender(),
              user.getAge(),
              Boolean.TRUE.equals(user.getProfileCompleted()) ? user.getAge() != null && user.getAge() < 18 : Boolean.FALSE,
              tags));
    }

    items.sort(Comparator.comparing(CoachStudentListResponse.StudentItem::name));
    return new CoachStudentListResponse(items);
  }

  private List<CoachStudentListResponse.PackageTag> buildActivePackageTags(
      List<CoursePackage> packages) {
    boolean hasExperience = false;
    int standardCount = 0;
    for (CoursePackage pkg : packages) {
      if (!PACKAGE_STATUS_ACTIVE.equals(pkg.getStatus())) {
        continue;
      }
      if (PACKAGE_MODE_EXPERIENCE.equals(pkg.getPackageMode())) {
        hasExperience = true;
      } else if (PACKAGE_MODE_STANDARD.equals(pkg.getPackageMode())) {
        standardCount++;
      }
    }
    List<CoachStudentListResponse.PackageTag> tags = new ArrayList<>();
    if (hasExperience) {
      tags.add(new CoachStudentListResponse.PackageTag("体验课", "experience"));
    }
    if (standardCount > 0) {
      tags.add(
          new CoachStudentListResponse.PackageTag(
              "正价套餐 x " + standardCount, "standard"));
    }
    return tags;
  }

  private String maskName(User user) {
    if (user.getName() != null) {
      return user.getName();
    }
    if (user.getPhone() != null && user.getPhone().length() >= 7) {
      return user.getPhone().substring(0, 3) + "****" + user.getPhone().substring(7);
    }
    return "学员" + (user.getId() != null ? user.getId() : "");
  }

  @Transactional(readOnly = true)
  public CoachStudentDetailResponse detail(Long coachId, CoachStudentDetailRequest request) {
    Long studentId = request.studentId();
    assertAssociation(coachId, studentId);

    User user =
        userMapper.selectById(studentId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    CoachStudentProfile profile = findOrCreateProfile(coachId, studentId);

    List<CoursePackage> packages = getCoachStudentPackages(coachId, studentId);
    int totalHours = packages.stream().mapToInt(CoursePackage::getTotalHours).sum();
    int remainingHours = packages.stream().mapToInt(CoursePackage::getAvailableCount).sum();

    String lastClassDate = null;
    LambdaQueryWrapper<Booking> bookingWrapper = new LambdaQueryWrapper<>();
    bookingWrapper
        .eq(Booking::getCoachId, coachId)
        .eq(Booking::getUserId, studentId)
        .in(Booking::getStatus, List.of("completed", "cancelled", "confirmed", "teaching"))
        .orderByDesc(Booking::getStartTime)
        .last("LIMIT 1");
    Booking lastBooking = bookingMapper.selectOne(bookingWrapper);
    if (lastBooking != null && lastBooking.getStartTime() != null) {
      lastClassDate = lastBooking.getStartTime().format(DATE_FORMATTER);
    }

    return new CoachStudentDetailResponse(
        studentId,
        new CoachStudentDetailResponse.UserProfile(
            user.getAvatarUrl(),
            maskName(user),
            maskPhone(user.getPhone()),
            user.getAge(),
            user.getGender(),
            user.getHasSwimBasis(),
            formatSwimStrokes(user.getSwimStrokes()),
            formatSwimYears(user.getSwimYears()),
            user.getPersonalDesc(),
            isMinor(user),
            user.getGuardianName(),
            maskPhone(user.getGuardianPhone())),
        new CoachStudentDetailResponse.CoachSlice(
            profile.getLearningStrokes(),
            profile.getSwimLevel(),
            profile.getBasics(),
            profile.getNotes()),
        new CoachStudentDetailResponse.Summary(totalHours, remainingHours, lastClassDate));
  }

  private boolean isMinor(User user) {
    return user.getAge() != null && user.getAge() < 18;
  }

  private String formatSwimStrokes(List<String> strokes) {
    if (strokes == null || strokes.isEmpty()) {
      return null;
    }
    return String.join("、", strokes);
  }

  private String formatSwimYears(Integer years) {
    if (years == null) {
      return null;
    }
    return years + "年";
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
  }

  @Transactional
  public void update(Long coachId, CoachStudentUpdateRequest request) {
    Long studentId = request.studentId();
    idempotencyHelper.checkAndLock(
        ActorType.coach.name(), coachId, request.idempotencyKey());
    try {
      assertAssociation(coachId, studentId);

      CoachStudentProfile profile = findOrCreateProfile(coachId, studentId);
      profile.setLearningStrokes(request.learningStrokes());
      profile.setSwimLevel(request.swimLevel());
      profile.setBasics(HtmlUtils.sanitizeDescription(StringUtils.defaultString(request.basics(), "")));
      profile.setNotes(HtmlUtils.sanitizeDescription(StringUtils.defaultString(request.notes(), "")));

      if (profile.getProfileId() == null) {
        coachStudentProfileMapper.insert(profile);
      } else {
        coachStudentProfileMapper.updateById(profile);
      }

      log.info("Coach student profile updated: coachId={}, studentId={}", coachId, studentId);
    } finally {
      idempotencyHelper.unlock(ActorType.coach.name(), coachId, request.idempotencyKey());
    }
  }

  @Transactional(readOnly = true)
  public CoachStudentPackageListResponse packageList(
      Long coachId, CoachStudentPackageListRequest request) {
    Long studentId = request.studentId();
    assertAssociation(coachId, studentId);

    List<CoursePackage> packages = getCoachStudentPackages(coachId, studentId);
    packages.sort(Comparator.comparing(CoursePackage::getCreatedAt).reversed());

    List<CoachStudentPackageListResponse.PackageItem> items =
        packages.stream()
            .map(
                pkg -> {
                  String validStart =
                      pkg.getCreatedAt() != null
                          ? pkg.getCreatedAt().format(DATE_FORMATTER)
                          : null;
                  String validEnd =
                      pkg.getExpireAt() != null
                          ? pkg.getExpireAt().format(DATE_FORMATTER)
                          : null;
                  return new CoachStudentPackageListResponse.PackageItem(
                      pkg.getId(),
                      pkg.getPackageName(),
                      pkg.getPackageMode(),
                      pkg.getStatus(),
                      mapPackageStatusLabel(pkg),
                      validStart,
                      validEnd,
                      pkg.getAvailableCount());
                })
            .toList();

    return new CoachStudentPackageListResponse(items);
  }

  private String mapPackageStatusLabel(CoursePackage pkg) {
    String status = pkg.getStatus();
    if (!PACKAGE_STATUS_ACTIVE.equals(status)) {
      return switch (status) {
        case "exhausted" -> "已使用";
        case "expired" -> "已过期";
        case "refunded" -> "已退款";
        case "frozen" -> "已冻结";
        default -> status;
      };
    }
    int consumed = pkg.getConsumedCount() == null ? 0 : pkg.getConsumedCount();
    return consumed == 0 ? "未使用" : "使用中";
  }

  @Transactional(readOnly = true)
  public CoachPackageDetailResponse packageDetail(
      Long coachId, CoachPackageDetailRequest request) {
    CoursePackage pkg = packageMapper.selectById(request.packageId());
    if (pkg == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }
    if (!coachId.equals(pkg.getCoachId())) {
      throw new BusinessException(ErrorCode.NOT_OWN_PACKAGE);
    }

    User user = userMapper.selectById(pkg.getUserId());

    LambdaQueryWrapper<Booking> bookingWrapper = new LambdaQueryWrapper<>();
    bookingWrapper
        .eq(Booking::getPackageId, pkg.getId())
        .orderByDesc(Booking::getStartTime);
    List<Booking> bookings = bookingMapper.selectList(bookingWrapper);

    return new CoachPackageDetailResponse(
        pkg.getId(),
        pkg.getPackageName(),
        pkg.getPackageMode(),
        pkg.getStatus(),
        mapPackageStatusLabel(pkg),
        pkg.getTeachingType(),
        formatStrokeNames(pkg.getStrokeIds()),
        pkg.getDurationMinutes(),
        pkg.getCreatedAt() != null ? pkg.getCreatedAt().format(DATE_FORMATTER) : null,
        pkg.getExpireAt() != null ? pkg.getExpireAt().format(DATE_FORMATTER) : null,
        pkg.getTotalHours(),
        pkg.getConsumedCount(),
        pkg.getAvailableCount(),
        pkg.getReservedCount(),
        pkg.getPaidAmount(),
        new CoachPackageDetailResponse.StudentMiniCard(
            user != null ? user.getId() : null,
            user != null ? maskName(user) : null,
            user != null ? user.getAvatarUrl() : null,
            user != null ? user.getAge() : null,
            user != null ? user.getGender() : null),
        bookings.stream().map(this::toUsageRecord).toList());
  }

  private String formatStrokeNames(List<Integer> strokeIds) {
    if (strokeIds == null || strokeIds.isEmpty()) {
      return null;
    }
    return strokeIds.stream()
        .map(id -> SwimStroke.toLabel(String.valueOf(id)))
        .filter(Objects::nonNull)
        .collect(Collectors.joining("、"));
  }

  private CoachPackageDetailResponse.UsageRecord toUsageRecord(Booking booking) {
    String status = booking.getStatus();
    String statusLabel = mapBookingStatusLabel(status);
    String cancelReason = null;
    if ("cancelled".equals(status) && booking.getCancelReason() != null) {
      cancelReason = mapCancelReason(booking.getCancelReason());
    }
    return new CoachPackageDetailResponse.UsageRecord(
        booking.getId(),
        booking.getStartTime() != null ? booking.getStartTime().format(DATE_TIME_FORMATTER) : null,
        status,
        statusLabel,
        1,
        cancelReason);
  }

  private String mapBookingStatusLabel(String status) {
    return switch (status) {
      case "completed" -> "已上课";
      case "cancelled" -> "已取消";
      case "booked", "confirmed", "teaching" -> "未上课";
      default -> status;
    };
  }

  private String mapCancelReason(Integer reason) {
    return switch (reason) {
      case 1 -> "学员取消";
      case 2 -> "教练离职";
      case 3 -> "学员旷课";
      case 4 -> "场馆闭馆";
      case 5 -> "教练请假";
      case 6 -> "套餐冻结";
      default -> null;
    };
  }

  private void assertAssociation(Long coachId, Long studentId) {
    LambdaQueryWrapper<CoursePackage> packageWrapper = new LambdaQueryWrapper<>();
    packageWrapper
        .eq(CoursePackage::getCoachId, coachId)
        .eq(CoursePackage::getUserId, studentId)
        .last("LIMIT 1");
    if (packageMapper.selectCount(packageWrapper) > 0) {
      return;
    }

    LambdaQueryWrapper<Booking> bookingWrapper = new LambdaQueryWrapper<>();
    bookingWrapper
        .eq(Booking::getCoachId, coachId)
        .eq(Booking::getUserId, studentId)
        .last("LIMIT 1");
    if (bookingMapper.selectCount(bookingWrapper) > 0) {
      return;
    }

    throw new BusinessException(ErrorCode.NOT_ASSOCIATED_STUDENT);
  }

  private CoachStudentProfile findOrCreateProfile(Long coachId, Long studentId) {
    LambdaQueryWrapper<CoachStudentProfile> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(CoachStudentProfile::getCoachId, coachId)
        .eq(CoachStudentProfile::getStudentUserId, studentId)
        .last("LIMIT 1");
    CoachStudentProfile profile = coachStudentProfileMapper.selectOne(wrapper);
    if (profile == null) {
      profile = new CoachStudentProfile();
      profile.setCoachId(coachId);
      profile.setStudentUserId(studentId);
    }
    return profile;
  }

  private List<CoursePackage> getCoachPackages(Long coachId) {
    LambdaQueryWrapper<CoursePackage> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(CoursePackage::getCoachId, coachId);
    return packageMapper.selectList(wrapper);
  }

  private List<CoursePackage> getCoachStudentPackages(Long coachId, Long studentId) {
    LambdaQueryWrapper<CoursePackage> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(CoursePackage::getCoachId, coachId).eq(CoursePackage::getUserId, studentId);
    return packageMapper.selectList(wrapper);
  }
}
