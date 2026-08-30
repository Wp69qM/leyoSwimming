package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.AdminResignationTicketDetailResponse;
import com.leyoswimming.dto.response.AdminResignationTicketListResponse;
import com.leyoswimming.entity.AdminUser;
import com.leyoswimming.entity.Booking;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachResignationAction;
import com.leyoswimming.entity.CoachResignationTicket;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AdminUserMapper;
import com.leyoswimming.repository.BookingMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachResignationActionMapper;
import com.leyoswimming.repository.CoachResignationTicketMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.ScheduleSlotMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import com.leyoswimming.util.RefundCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachResignationService {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("booked", "confirmed");

  private final CoachResignationTicketMapper ticketMapper;
  private final CoachResignationActionMapper actionMapper;
  private final CoachMapper coachMapper;
  private final PackageMapper packageMapper;
  private final RefundRecordMapper refundRecordMapper;
  private final BookingMapper bookingMapper;
  private final ScheduleSlotMapper scheduleSlotMapper;
  private final UserMapper userMapper;
  private final AdminUserMapper adminUserMapper;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional(readOnly = true)
  public AdminResignationTicketListResponse list(String status, Integer page, Integer pageSize) {
    return list(status, page, pageSize, null, null, null);
  }

  @Transactional(readOnly = true)
  public AdminResignationTicketListResponse list(
      String status,
      Integer page,
      Integer pageSize,
      String keyword,
      String submitStartDate,
      String submitEndDate) {
    int current = page == null || page < 1 ? DEFAULT_PAGE : page;
    int size = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;

    List<Long> filteredCoachIds = null;
    if (StringUtils.hasText(keyword)) {
      LambdaQueryWrapper<Coach> coachWrapper =
          new LambdaQueryWrapper<Coach>()
              .like(Coach::getName, keyword.trim())
              .last("LIMIT 200");
      filteredCoachIds =
          coachMapper.selectList(coachWrapper).stream()
              .map(Coach::getId)
              .distinct()
              .toList();
      if (filteredCoachIds.isEmpty()) {
        return new AdminResignationTicketListResponse(
            Collections.emptyList(), 0L, current, size);
      }
    }

    LambdaQueryWrapper<CoachResignationTicket> wrapper =
        new LambdaQueryWrapper<CoachResignationTicket>()
            .orderByDesc(CoachResignationTicket::getCreatedAt);
    if (status != null && !status.isBlank()) {
      wrapper.eq(CoachResignationTicket::getStatus, status);
    }
    if (filteredCoachIds != null) {
      wrapper.in(CoachResignationTicket::getCoachId, filteredCoachIds);
    }
    LocalDateTime startAt = parseDateStart(submitStartDate);
    LocalDateTime endAt = parseDateEnd(submitEndDate);
    if (startAt != null) {
      wrapper.ge(CoachResignationTicket::getSubmittedAt, startAt);
    }
    if (endAt != null) {
      wrapper.lt(CoachResignationTicket::getSubmittedAt, endAt);
    }

    Page<CoachResignationTicket> pageResult =
        ticketMapper.selectPage(new Page<>(current, size), wrapper);
    List<Long> coachIds =
        pageResult.getRecords().stream()
            .map(CoachResignationTicket::getCoachId)
            .distinct()
            .toList();
    Map<Long, Coach> coachMap =
        coachIds.isEmpty()
            ? Map.of()
            : coachMapper.selectBatchIds(coachIds).stream()
                .collect(Collectors.toMap(Coach::getId, Function.identity()));

    List<AdminResignationTicketListResponse.TicketItem> items =
        pageResult.getRecords().stream()
            .map(
                ticket -> {
                  Coach coach = coachMap.get(ticket.getCoachId());
                  Long studentCount =
                      packageMapper.countActiveStudentsByCoachId(ticket.getCoachId());
                  return new AdminResignationTicketListResponse.TicketItem(
                      ticket.getId(),
                      ticket.getTicketNo(),
                      ticket.getCoachId(),
                      coach != null ? coach.getName() : null,
                      coach != null ? decryptPhone(coach.getPhone()) : null,
                      ticket.getReason(),
                      ticket.getStatus(),
                      ticket.getTotalPackages(),
                      ticket.getHandledPackages(),
                      studentCount != null ? studentCount.intValue() : 0,
                      ticket.getSubmittedAt(),
                      ticket.getCreatedAt(),
                      coachJoinedAt(coach));
                })
            .toList();

    return new AdminResignationTicketListResponse(
        items, pageResult.getTotal(), current, size);
  }

  private LocalDateTime parseDateStart(String date) {
    if (!StringUtils.hasText(date)) {
      return null;
    }
    try {
      return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始日期格式错误");
    }
  }

  private LocalDateTime parseDateEnd(String date) {
    if (!StringUtils.hasText(date)) {
      return null;
    }
    try {
      return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
          .plusDays(1)
          .atStartOfDay();
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "结束日期格式错误");
    }
  }

  @Transactional(readOnly = true)
  public AdminResignationTicketDetailResponse detail(Long ticketId) {
    CoachResignationTicket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return buildDetailResponse(ticket);
  }

  @Transactional
  public void approve(Long adminId, Long ticketId, String comment) {
    ensurePermission(adminId);

    CoachResignationTicket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    if (!"pending_audit".equals(ticket.getStatus()) && !"processing".equals(ticket.getStatus())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_PENDING_AUDIT);
    }

    if (StringUtils.hasText(comment)) {
      ticket.setReason(
          Objects.requireNonNullElse(ticket.getReason(), "") + " | 审批意见：" + comment);
    }

    Long coachId = ticket.getCoachId();
    List<CoursePackage> activePackages = packageMapper.findActiveByCoachId(coachId);
    List<CoachResignationAction> actions = actionMapper.findByTicketId(ticketId);
    Map<Long, CoachResignationAction> actionMap =
        actions.stream()
            .collect(Collectors.toMap(CoachResignationAction::getPackageId, Function.identity()));

    boolean allActionsRegistered =
        activePackages.isEmpty()
            || activePackages.stream().allMatch(p -> actionMap.containsKey(p.getId()));
    if (!allActionsRegistered) {
      throw new BusinessException(ErrorCode.CHECKLIST_NOT_PASSED);
    }

    // MVP 阶段排班清空功能未实现，审批时自动清空未来时段，不再前置拦截
    LocalDateTime now = LocalDateTime.now();

    for (CoursePackage pkg : activePackages) {
      CoachResignationAction action = actionMap.get(pkg.getId());
      if (action == null) {
        continue;
      }
      switch (action.getAction()) {
        case "transfer" -> {
          pkg.setCoachId(action.getTargetCoachId());
          action.setStatus("approved");
        }
        case "refund" -> {
          BigDecimal refundAmount = RefundCalculator.calculateResignationRefund(pkg);
          createOrUpdateRefundRecord(pkg.getId(), ticketId, refundAmount);
          pkg.setStatus("frozen");
          pkg.setFrozenReason("coach_resigned");
          action.setStatus("approved");
        }
        case "continue" -> {
          action.setStatus("approved");
        }
        default -> throw new BusinessException(ErrorCode.BAD_REQUEST);
      }
      actionMapper.updateById(action);
      packageMapper.updateById(pkg);
    }

    List<Booking> futureBookings = bookingMapper.findFutureActiveByCoachId(coachId, now);
    for (Booking booking : futureBookings) {
      CoursePackage pkg = packageMapper.selectById(booking.getPackageId());
      if (pkg != null) {
        int reserved = pkg.getReservedCount() == null ? 0 : pkg.getReservedCount();
        int available = pkg.getAvailableCount() == null ? 0 : pkg.getAvailableCount();
        if (reserved > 0) {
          pkg.setReservedCount(reserved - 1);
          pkg.setAvailableCount(available + 1);
          packageMapper.updateById(pkg);
        }
      }
    }
    bookingMapper.cancelFutureByCoachId(coachId, now);
    scheduleSlotMapper.hideFutureByCoachId(coachId, now);

    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    coach.setStatus(CoachStatus.RESIGNED.getValue());
    coach.setPhoneHash(null);
    coachMapper.updateById(coach);

    ticket.setStatus("approved");
    ticket.setScheduleCleared(true);
    ticket.setSettlementStatus(1);
    ticketMapper.updateById(ticket);

    log.info(
        "Admin approved coach resignation: adminId={}, ticketId={}, coachId={}",
        adminId,
        ticketId,
        coachId);
  }

  @Transactional
  public void reject(Long adminId, Long ticketId, String reason) {
    ensurePermission(adminId);

    CoachResignationTicket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    if (!"pending_audit".equals(ticket.getStatus()) && !"processing".equals(ticket.getStatus())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_PENDING_AUDIT);
    }

    Long coachId = ticket.getCoachId();
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "教练不存在");
    }
    coach.setStatus(CoachStatus.APPROVED.getValue());
    coachMapper.updateById(coach);

    ticket.setStatus("rejected");
    ticket.setReason(Objects.requireNonNullElse(ticket.getReason(), "") + " | 拒绝原因：" + reason);
    ticketMapper.updateById(ticket);

    log.info(
        "Admin rejected coach resignation: adminId={}, ticketId={}, coachId={}",
        adminId,
        ticketId,
        coachId);
  }

  private void ensurePermission(Long adminId) {
    AdminUser admin = adminUserMapper.selectById(adminId);
    if (admin == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    if (!isCoachAdminRole(admin.getRole())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  private boolean isCoachAdminRole(String role) {
    return switch (Objects.requireNonNullElse(role, "").toLowerCase()) {
      case "super_admin", "admin", "coach_manager" -> true;
      default -> false;
    };
  }

  private AdminResignationTicketDetailResponse buildDetailResponse(
      CoachResignationTicket ticket) {
    Long coachId = ticket.getCoachId();
    Coach coach = coachMapper.selectById(coachId);
    List<CoursePackage> activePackages = packageMapper.findActiveByCoachId(coachId);
    List<CoachResignationAction> actions = actionMapper.findByTicketId(ticket.getId());
    Map<Long, CoachResignationAction> actionMap =
        actions.stream()
            .collect(Collectors.toMap(CoachResignationAction::getPackageId, Function.identity()));

    boolean allActionsRegistered =
        activePackages.isEmpty()
            || activePackages.stream().allMatch(p -> actionMap.containsKey(p.getId()));

    AdminResignationTicketDetailResponse.CoachInfo coachInfo =
        new AdminResignationTicketDetailResponse.CoachInfo(
            coachId,
            coach != null ? coach.getName() : null,
            coach != null ? decryptPhone(coach.getPhone()) : null,
            coach != null ? coach.getStatus() : null,
            coach != null ? coach.getSubmittedAt() : null,
            coachJoinedAt(coach));

    Set<Long> targetCoachIds =
        actions.stream()
            .map(CoachResignationAction::getTargetCoachId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Long, Coach> targetCoachMap =
        targetCoachIds.isEmpty()
            ? Map.of()
            : coachMapper.selectBatchIds(targetCoachIds).stream()
                .collect(Collectors.toMap(Coach::getId, Function.identity()));

    List<AdminResignationTicketDetailResponse.PackageItem> items =
        activePackages.stream()
            .map(
                pkg -> {
                  CoachResignationAction action = actionMap.get(pkg.getId());
                  User user = userMapper.selectById(pkg.getUserId());
                  BigDecimal refundAmount = null;
                  if (action != null && "refund".equals(action.getAction())) {
                    refundAmount = RefundCalculator.calculateResignationRefund(pkg);
                  }
                  Coach targetCoach =
                      action != null && action.getTargetCoachId() != null
                          ? targetCoachMap.get(action.getTargetCoachId())
                          : null;
                  return new AdminResignationTicketDetailResponse.PackageItem(
                      pkg.getId(),
                      pkg.getPackageNo(),
                      pkg.getUserId(),
                      user != null ? user.getName() : null,
                      pkg.getTotalHours(),
                      pkg.getAvailableCount(),
                      pkg.getReservedCount(),
                      pkg.getPricePerHour(),
                      action != null ? action.getAction() : null,
                      action != null ? action.getTargetCoachId() : null,
                      targetCoach != null ? targetCoach.getName() : null,
                      targetCoach != null ? decryptPhone(targetCoach.getPhone()) : null,
                      refundAmount,
                      coach != null ? coach.getName() : null,
                      action != null ? action.getUpdatedAt() : null);
                })
            .toList();

    // MVP 阶段未实现教练费结算和排班清空功能，默认视为已通过
    AdminResignationTicketDetailResponse.Checklist checklist =
        new AdminResignationTicketDetailResponse.Checklist(
            allActionsRegistered, true, true);

    return new AdminResignationTicketDetailResponse(
        ticket.getId(),
        ticket.getTicketNo(),
        ticket.getStatus(),
        ticket.getReason(),
        ticket.getTotalPackages(),
        ticket.getHandledPackages(),
        ticket.getScheduleCleared(),
        ticket.getSettlementStatus(),
        ticket.getSubmittedAt(),
        coachInfo,
        items,
        checklist);
  }

  private void createOrUpdateRefundRecord(Long packageId, Long ticketId, BigDecimal amount) {
    LambdaQueryWrapper<RefundRecord> wrapper =
        new LambdaQueryWrapper<RefundRecord>()
            .eq(RefundRecord::getPackageId, packageId)
            .eq(RefundRecord::getTicketId, ticketId);
    RefundRecord record = refundRecordMapper.selectOne(wrapper);
    if (record == null) {
      record = new RefundRecord();
      record.setPackageId(packageId);
      record.setTicketId(ticketId);
      record.setRefundAmount(amount);
      record.setStatus(0);
      refundRecordMapper.insert(record);
    } else {
      record.setRefundAmount(amount);
      refundRecordMapper.updateById(record);
    }
  }

  private String decryptPhone(String encryptedPhone) {
    if (encryptedPhone == null) {
      return null;
    }
    try {
      return phoneEncryptor.decrypt(encryptedPhone);
    } catch (Exception e) {
      return null;
    }
  }

  private LocalDateTime coachJoinedAt(Coach coach) {
    if (coach == null) {
      return null;
    }
    return coach.getApprovedAt() != null ? coach.getApprovedAt() : coach.getCreatedAt();
  }
}
