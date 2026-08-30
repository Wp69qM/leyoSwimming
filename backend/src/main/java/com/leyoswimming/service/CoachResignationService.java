package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.CoachResignationApplyResponse;
import com.leyoswimming.dto.response.CoachResignationDetailResponse;
import com.leyoswimming.dto.response.CoachResignationPackageActionResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoachResignationAction;
import com.leyoswimming.entity.CoachResignationTicket;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CoachResignationActionMapper;
import com.leyoswimming.repository.CoachResignationTicketMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.util.PhoneEncryptor;
import com.leyoswimming.util.RefundCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachResignationService {

  private static final Set<String> VALID_ACTIONS = Set.of("refund", "transfer", "continue");

  private final CoachMapper coachMapper;
  private final PackageMapper packageMapper;
  private final CoachResignationTicketMapper ticketMapper;
  private final CoachResignationActionMapper actionMapper;
  private final RefundRecordMapper refundRecordMapper;
  private final UserMapper userMapper;
  private final PhoneEncryptor phoneEncryptor;

  @Transactional
  public CoachResignationApplyResponse apply(Long coachId, String reason, String idempotencyKey) {
    Coach coach = coachMapper.selectById(coachId);
    if (coach == null) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }
    if (coach.getStatus() != CoachStatus.APPROVED.getValue()) {
      throw new BusinessException(ErrorCode.COACH_STATUS_NOT_ALLOWED);
    }

    CoachResignationTicket existing = ticketMapper.findActiveByCoachId(coachId);
    if (existing != null) {
      throw new BusinessException(ErrorCode.RESIGNATION_ALREADY_PENDING);
    }

    List<CoursePackage> activePackages = packageMapper.findActiveByCoachId(coachId);

    CoachResignationTicket ticket = new CoachResignationTicket();
    ticket.setCoachId(coachId);
    ticket.setTicketNo(generateTicketNo());
    ticket.setReason(reason);
    ticket.setStatus("processing");
    ticket.setTotalPackages(activePackages.size());
    ticket.setHandledPackages(0);
    ticket.setScheduleCleared(false);
    ticket.setSettlementStatus(0);
    ticketMapper.insert(ticket);

    coach.setStatus(CoachStatus.RESIGNING.getValue());
    coachMapper.updateById(coach);

    log.info("Coach resignation applied: coachId={}, ticketNo={}", coachId, ticket.getTicketNo());
    return new CoachResignationApplyResponse(
        ticket.getId(),
        ticket.getTicketNo(),
        activePackages.size(),
        "离职申请已提交，请处理学员套餐");
  }

  public CoachResignationDetailResponse detail(Long coachId, Long ticketId) {
    CoachResignationTicket ticket;
    if (ticketId != null) {
      ticket = ticketMapper.selectById(ticketId);
      if (ticket == null || !ticket.getCoachId().equals(coachId)) {
        return buildEmptyDetailResponse();
      }
    } else {
      ticket = ticketMapper.findActiveByCoachId(coachId);
      if (ticket == null) {
        return buildDraftDetailResponse(coachId);
      }
    }
    return buildDetailResponse(ticket);
  }

  @Transactional
  public CoachResignationPackageActionResponse registerAction(
      Long coachId,
      Long ticketId,
      Long packageId,
      String action,
      Long targetCoachId) {
    if (!VALID_ACTIONS.contains(action)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    CoachResignationTicket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null || !ticket.getCoachId().equals(coachId)) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    if (!"processing".equals(ticket.getStatus())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_PROCESSING);
    }

    CoursePackage pkg = packageMapper.selectById(packageId);
    if (pkg == null || !pkg.getCoachId().equals(coachId)) {
      throw new BusinessException(ErrorCode.NOT_OWN_PACKAGE);
    }

    if ("transfer".equals(action) && targetCoachId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }

    CoachResignationAction existing =
        actionMapper.findByTicketIdAndPackageId(ticketId, packageId);
    BigDecimal refundAmount = null;
    if (existing == null) {
      existing = new CoachResignationAction();
      existing.setTicketId(ticketId);
      existing.setPackageId(packageId);
    }
    existing.setAction(action);
    existing.setTargetCoachId(targetCoachId);
    existing.setStatus("registered");
    if (existing.getId() == null) {
      actionMapper.insert(existing);
    } else {
      actionMapper.updateById(existing);
    }

    if ("refund".equals(action)) {
      refundAmount = RefundCalculator.calculateResignationRefund(pkg);
      createOrUpdateRefundRecord(packageId, ticketId, refundAmount);
    }

    updateHandledPackages(ticket);

    return new CoachResignationPackageActionResponse(
        existing.getId(), packageId, action, refundAmount);
  }

  @Transactional
  public void submit(Long coachId, Long ticketId) {
    CoachResignationTicket ticket = ticketMapper.selectById(ticketId);
    if (ticket == null || !ticket.getCoachId().equals(coachId)) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    if (!"processing".equals(ticket.getStatus())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_PROCESSING);
    }

    List<CoursePackage> activePackages = packageMapper.findActiveByCoachId(coachId);
    Map<Long, CoachResignationAction> actionMap =
        actionMapper.findByTicketId(ticketId).stream()
            .collect(Collectors.toMap(CoachResignationAction::getPackageId, Function.identity()));

    for (CoursePackage pkg : activePackages) {
      if (!actionMap.containsKey(pkg.getId())) {
        BigDecimal refundAmount = RefundCalculator.calculateResignationRefund(pkg);
        createAction(ticketId, pkg.getId(), "refund", null);
        createOrUpdateRefundRecord(pkg.getId(), ticketId, refundAmount);
      }
    }

    ticket.setStatus("pending_audit");
    ticket.setSubmittedAt(LocalDateTime.now());
    ticket.setHandledPackages(activePackages.size());
    ticketMapper.updateById(ticket);

    log.info("Coach resignation submitted to admin: coachId={}, ticketId={}", coachId, ticketId);
  }

  private CoachResignationDetailResponse buildDetailResponse(CoachResignationTicket ticket) {
    List<CoursePackage> packages = packageMapper.findActiveByCoachId(ticket.getCoachId());
    List<CoachResignationAction> actions = actionMapper.findByTicketId(ticket.getId());
    Map<Long, CoachResignationAction> actionMap =
        actions.stream()
            .collect(Collectors.toMap(CoachResignationAction::getPackageId, Function.identity()));

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

    List<CoachResignationDetailResponse.PackageItem> items =
        packages.stream()
            .map(
                pkg -> {
                  CoachResignationAction action = actionMap.get(pkg.getId());
                  User user = userMapper.selectById(pkg.getUserId());
                  Coach targetCoach =
                      action != null && action.getTargetCoachId() != null
                          ? targetCoachMap.get(action.getTargetCoachId())
                          : null;
                  return new CoachResignationDetailResponse.PackageItem(
                      pkg.getId(),
                      pkg.getPackageNo(),
                      pkg.getUserId(),
                      user != null ? user.getName() : null,
                      pkg.getPackageName(),
                      pkg.getPackageMode(),
                      pkg.getTotalHours(),
                      pkg.getAvailableCount(),
                      pkg.getReservedCount(),
                      pkg.getPricePerHour(),
                      action != null ? action.getAction() : null,
                      action != null ? action.getTargetCoachId() : null,
                      targetCoach != null ? targetCoach.getName() : null,
                      targetCoach != null ? decryptPhone(targetCoach.getPhone()) : null);
                })
            .collect(Collectors.toList());

    int totalPackages = items.size();
    int handledPackages = (int) packages.stream().filter(pkg -> actionMap.containsKey(pkg.getId())).count();

    return new CoachResignationDetailResponse(
        ticket.getId(),
        ticket.getTicketNo(),
        ticket.getStatus(),
        ticket.getReason(),
        totalPackages,
        handledPackages,
        ticket.getSubmittedAt(),
        items);
  }

  private CoachResignationDetailResponse buildDraftDetailResponse(Long coachId) {
    List<CoursePackage> packages = packageMapper.findActiveByCoachId(coachId);

    List<CoachResignationDetailResponse.PackageItem> items =
        packages.stream()
            .map(
                pkg -> {
                  User user = userMapper.selectById(pkg.getUserId());
                  return new CoachResignationDetailResponse.PackageItem(
                      pkg.getId(),
                      pkg.getPackageNo(),
                      pkg.getUserId(),
                      user != null ? user.getName() : null,
                      pkg.getPackageName(),
                      pkg.getPackageMode(),
                      pkg.getTotalHours(),
                      pkg.getAvailableCount(),
                      pkg.getReservedCount(),
                      pkg.getPricePerHour(),
                      null,
                      null,
                      null,
                      null);
                })
            .collect(Collectors.toList());

    return new CoachResignationDetailResponse(
        null, null, "none", null, items.size(), 0, null, items);
  }

  private CoachResignationDetailResponse buildEmptyDetailResponse() {
    return new CoachResignationDetailResponse(
        null, null, "none", null, 0, 0, null, List.of());
  }

  private void createAction(Long ticketId, Long packageId, String action, Long targetCoachId) {
    CoachResignationAction record = new CoachResignationAction();
    record.setTicketId(ticketId);
    record.setPackageId(packageId);
    record.setAction(action);
    record.setTargetCoachId(targetCoachId);
    record.setStatus("registered");
    actionMapper.insert(record);
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

  private void updateHandledPackages(CoachResignationTicket ticket) {
    long count =
        actionMapper.findByTicketId(ticket.getId()).stream()
            .filter(a -> !"continue".equals(a.getAction()) || a.getTargetCoachId() != null)
            .filter(a -> Objects.nonNull(a.getAction()))
            .count();
    ticket.setHandledPackages((int) count);
    ticketMapper.updateById(ticket);
  }

  private String generateTicketNo() {
    return "CR"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
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
}
