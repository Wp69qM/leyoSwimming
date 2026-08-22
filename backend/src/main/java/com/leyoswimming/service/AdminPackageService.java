package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.AdminPackageExtendRequest;
import com.leyoswimming.dto.request.AdminPackageFreezeRequest;
import com.leyoswimming.dto.request.AdminPackageListRequest;
import com.leyoswimming.dto.request.AdminPackageRefundRequest;
import com.leyoswimming.dto.request.AdminPackageUnfreezeRequest;
import com.leyoswimming.dto.response.AdminPackageDetailResponse;
import com.leyoswimming.dto.response.AdminPackageListItemResponse;
import com.leyoswimming.dto.response.AdminPackageListResponse;
import com.leyoswimming.dto.response.AdminPackageOperationResponse;
import com.leyoswimming.entity.AuditLog;
import com.leyoswimming.entity.Booking;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.AuditLogMapper;
import com.leyoswimming.repository.BookingMapper;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPackageService {

  private static final String REFUND_ORDER_PREFIX = "R";
  private static final String REFUND_FROZEN_REASON = "refund_pending";
  private static final String ADMIN_FROZEN_REASON = "admin_frozen";
  private static final String STATUS_ACTIVE = "active";
  private static final String STATUS_FROZEN = "frozen";
  private static final String STATUS_EXPIRED = "expired";
  private static final String STATUS_EXHAUSTED = "exhausted";
  private static final String STATUS_REFUNDED = "refunded";
  private static final String ACTOR_TYPE = "admin";
  private static final String TARGET_TYPE = "package";
  private static final String ACTION_FREEZE = "ADMIN_FREEZE_PACKAGE";
  private static final String ACTION_UNFREEZE = "ADMIN_UNFREEZE_PACKAGE";
  private static final String ACTION_EXTEND = "ADMIN_EXTEND_PACKAGE";
  private static final String ACTION_REQUEST_REFUND = "ADMIN_REQUEST_PACKAGE_REFUND";
  private static final int REFUND_RECORD_STATUS_PENDING = 0;
  private static final String LOCK_RESOURCE_REFUND_APPLY = "refund:apply";
  private static final Duration LOCK_TTL_REFUND_APPLY = Duration.ofSeconds(10);

  private final PackageMapper packageMapper;
  private final OrderMapper orderMapper;
  private final RefundRecordMapper refundRecordMapper;
  private final BookingMapper bookingMapper;
  private final UserMapper userMapper;
  private final CoachMapper coachMapper;
  private final AuditLogMapper auditLogMapper;
  private final PackageService packageService;
  private final AdminPermissionHelper permissionHelper;
  private final DistributedLockHelper lockHelper;

  @Transactional(readOnly = true)
  public AdminPackageListResponse list(Long adminId, AdminPackageListRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_READ);

    LambdaQueryWrapper<CoursePackage> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(request.status())) {
      wrapper.eq(CoursePackage::getStatus, request.status().trim());
    }
    if (StringUtils.isNotBlank(request.packageMode())) {
      wrapper.eq(CoursePackage::getPackageMode, request.packageMode().trim());
    }
    if (StringUtils.isNotBlank(request.teachingType())) {
      wrapper.eq(CoursePackage::getTeachingType, request.teachingType().trim());
    }
    if (request.expireAtStart() != null) {
      wrapper.ge(CoursePackage::getExpireAt, LocalDateTime.of(request.expireAtStart(), LocalTime.MIN));
    }
    if (request.expireAtEnd() != null) {
      wrapper.le(CoursePackage::getExpireAt, LocalDateTime.of(request.expireAtEnd(), LocalTime.MAX));
    }
    if (StringUtils.isNotBlank(request.keyword())) {
      String keyword = request.keyword().trim();
      wrapper.and(
          w ->
              w.eq(CoursePackage::getId, keyword)
                  .or()
                  .like(CoursePackage::getUserId, keyword)
                  .or()
                  .like(CoursePackage::getCoachId, keyword));
    }
    wrapper.orderByDesc(CoursePackage::getCreatedAt);

    Page<CoursePackage> page = new Page<>(request.page(), request.pageSize());
    Page<CoursePackage> result = packageMapper.selectPage(page, wrapper);

    List<CoursePackage> records = result.getRecords();
    if (records.isEmpty()) {
      return new AdminPackageListResponse(List.of(), result.getTotal(), request.page(), request.pageSize());
    }

    Map<Long, User> userMap = findUserMap(records);
    Map<Long, Coach> coachMap = findCoachMap(records);

    List<AdminPackageListItemResponse> list =
        records.stream()
            .map(
                pkg -> {
                  User user = userMap.get(pkg.getUserId());
                  Coach coach = coachMap.get(pkg.getCoachId());
                  return new AdminPackageListItemResponse(
                      pkg.getId(),
                      pkg.getPackageNo(),
                      pkg.getUserId(),
                      user == null ? null : user.getName(),
                      pkg.getCoachId(),
                      coach == null ? null : coach.getName(),
                      pkg.getPackageMode(),
                      pkg.getTeachingType(),
                      pkg.getStatus(),
                      pkg.getFrozenReason(),
                      pkg.getTotalHours(),
                      pkg.getConsumedCount(),
                      pkg.getAvailableCount(),
                      pkg.getReservedCount(),
                      pkg.getExpireAt(),
                      pkg.getCreatedAt(),
                      pkg.getRefundEnabled(),
                      pkg.getRefundValidDays(),
                      pkg.getRefundRatio(),
                      packageService.calculateRefundAmount(pkg),
                      pkg.getVersion());
                })
            .toList();

    return new AdminPackageListResponse(list, result.getTotal(), request.page(), request.pageSize());
  }

  @Transactional(readOnly = true)
  public AdminPackageDetailResponse detail(Long adminId, Long packageId) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_READ);

    CoursePackage coursePackage = findPackage(packageId);
    User user = userMapper.selectById(coursePackage.getUserId());
    Coach coach =
        coursePackage.getCoachId() == null ? null : coachMapper.selectById(coursePackage.getCoachId());

    List<AdminPackageDetailResponse.ConsumptionRecord> consumptionRecords =
        buildConsumptionRecords(packageId);
    List<AdminPackageDetailResponse.OrderInfo> relatedOrders = buildRelatedOrders(packageId);

    return new AdminPackageDetailResponse(
        coursePackage.getId(),
        coursePackage.getPackageNo(),
        coursePackage.getUserId(),
        user == null ? null : user.getName(),
        user == null ? null : user.getPhone(),
        coursePackage.getCoachId(),
        coach == null ? null : coach.getName(),
        coursePackage.getPackageMode(),
        coursePackage.getPackageName(),
        coursePackage.getTeachingType(),
        coursePackage.getStrokeIds(),
        coursePackage.getDurationMinutes(),
        coursePackage.getValidDays(),
        coursePackage.getStatus(),
        coursePackage.getFrozenReason(),
        coursePackage.getExtendReason(),
        coursePackage.getTotalHours(),
        coursePackage.getConsumedCount(),
        coursePackage.getReservedCount(),
        coursePackage.getAvailableCount(),
        coursePackage.getPricePerHour(),
        coursePackage.getPaidAmount(),
        coursePackage.getOriginalPrice(),
        coursePackage.getRefundEnabled(),
        coursePackage.getRefundRatio(),
        coursePackage.getRefundValidDays(),
        packageService.calculateRefundAmount(coursePackage),
        coursePackage.getPendingHandoverAt(),
        coursePackage.getExpireAt(),
        coursePackage.getExhaustedAt(),
        coursePackage.getRefundedAt(),
        coursePackage.getCreatedAt(),
        coursePackage.getVersion(),
        consumptionRecords,
        relatedOrders);
  }

  @Transactional
  public AdminPackageOperationResponse freeze(Long adminId, AdminPackageFreezeRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);

    CoursePackage coursePackage = findPackage(request.packageId());
    validateVersion(coursePackage, request.version());

    if (!STATUS_ACTIVE.equals(coursePackage.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_ACTIVE);
    }

    LocalDateTime now = LocalDateTime.now();
    int released = releaseReservedAndCancelBookings(coursePackage, now);

    CoursePackage updatePackage = new CoursePackage();
    updatePackage.setId(coursePackage.getId());
    updatePackage.setVersion(request.version());
    updatePackage.setStatus(STATUS_FROZEN);
    updatePackage.setFrozenReason(ADMIN_FROZEN_REASON);
    if (released > 0) {
      updatePackage.setReservedCount(0);
      updatePackage.setAvailableCount(
          (coursePackage.getAvailableCount() == null ? 0 : coursePackage.getAvailableCount())
              + (coursePackage.getReservedCount() == null ? 0 : coursePackage.getReservedCount()));
    }

    int affected = packageMapper.updateById(updatePackage);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.PACKAGE_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        coursePackage.getId(),
        ACTION_FREEZE,
        snapshot(coursePackage),
        snapshot(updatePackage),
        request.reasonDetail(),
        null);

    log.info(
        "Admin freeze package: adminId={}, packageId={}, releasedBookings={}",
        adminId,
        coursePackage.getId(),
        released);

    return AdminPackageOperationResponse.of("套餐已冻结");
  }

  @Transactional
  public AdminPackageOperationResponse unfreeze(Long adminId, AdminPackageUnfreezeRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);

    CoursePackage coursePackage = findPackage(request.packageId());
    validateVersion(coursePackage, request.version());

    if (!STATUS_FROZEN.equals(coursePackage.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FROZEN);
    }
    if (REFUND_FROZEN_REASON.equals(coursePackage.getFrozenReason())) {
      throw new BusinessException(
          ErrorCode.PACKAGE_STATUS_NOT_ALLOWED, "退款审批中的套餐不允许直接解冻");
    }

    CoursePackage updatePackage = new CoursePackage();
    updatePackage.setId(coursePackage.getId());
    updatePackage.setVersion(request.version());
    updatePackage.setStatus(STATUS_ACTIVE);
    updatePackage.setFrozenReason(null);

    int affected = packageMapper.updateById(updatePackage);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.PACKAGE_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        coursePackage.getId(),
        ACTION_UNFREEZE,
        snapshot(coursePackage),
        snapshot(updatePackage),
        null,
        null);

    log.info("Admin unfreeze package: adminId={}, packageId={}", adminId, coursePackage.getId());

    return AdminPackageOperationResponse.of("套餐已解冻");
  }

  @Transactional
  public AdminPackageOperationResponse extend(Long adminId, AdminPackageExtendRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);

    CoursePackage coursePackage = findPackage(request.packageId());
    validateVersion(coursePackage, request.version());

    if (!STATUS_ACTIVE.equals(coursePackage.getStatus())
        && !STATUS_EXPIRED.equals(coursePackage.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_EXTENDABLE);
    }

    int remaining =
        (coursePackage.getAvailableCount() == null ? 0 : coursePackage.getAvailableCount())
            + (coursePackage.getReservedCount() == null ? 0 : coursePackage.getReservedCount());
    if (remaining <= 0) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_EXTENDABLE);
    }

    if (request.newExpireAt() == null || !request.newExpireAt().isAfter(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.INVALID_EXTENSION_REASON, "新到期时间必须晚于当前时间");
    }
    if (coursePackage.getExpireAt() != null
        && !request.newExpireAt().isAfter(coursePackage.getExpireAt())) {
      throw new BusinessException(ErrorCode.INVALID_EXTENSION_REASON, "新到期时间必须晚于原到期时间");
    }

    String reason = StringUtils.trimToNull(request.reason());
    if (reason == null) {
      throw new BusinessException(ErrorCode.INVALID_EXTENSION_REASON);
    }

    CoursePackage updatePackage = new CoursePackage();
    updatePackage.setId(coursePackage.getId());
    updatePackage.setVersion(request.version());
    updatePackage.setStatus(STATUS_ACTIVE);
    updatePackage.setExpireAt(request.newExpireAt());
    updatePackage.setExtendReason(reason);

    int affected = packageMapper.updateById(updatePackage);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.PACKAGE_CONCURRENTLY_UPDATED);
    }

    writeAuditLog(
        adminId,
        coursePackage.getId(),
        ACTION_EXTEND,
        snapshot(coursePackage),
        snapshot(updatePackage),
        reason,
        null);

    log.info(
        "Admin extend package: adminId={}, packageId={}, newExpireAt={}",
        adminId,
        coursePackage.getId(),
        request.newExpireAt());

    return AdminPackageOperationResponse.of("套餐已延期");
  }

  @Transactional
  public AdminPackageOperationResponse requestRefund(
      Long adminId, AdminPackageRefundRequest request) {
    permissionHelper.checkPermission(adminId, AdminPermissionHelper.PERM_PACKAGE_WRITE);

    CoursePackage coursePackage = findPackage(request.packageId());
    validateVersion(coursePackage, request.version());

    if (!STATUS_ACTIVE.equals(coursePackage.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_STATUS_NOT_ALLOWED, "只有 active 状态的套餐可申请退款");
    }
    if (Boolean.FALSE.equals(coursePackage.getRefundEnabled())) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "该套餐不允许退款");
    }
    if (coursePackage.getRefundValidDays() != null
        && coursePackage.getRefundValidDays() > 0
        && coursePackage.getCreatedAt() != null) {
      LocalDateTime deadline =
          coursePackage.getCreatedAt().plusDays(coursePackage.getRefundValidDays());
      if (LocalDateTime.now().isAfter(deadline)) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "已超过退款有效期");
      }
    }

    LockToken lock =
        lockHelper.lock(
            LOCK_RESOURCE_REFUND_APPLY,
            "package:" + request.packageId(),
            LOCK_TTL_REFUND_APPLY);
    try {
      LambdaQueryWrapper<Order> pendingRefundWrapper = new LambdaQueryWrapper<>();
      pendingRefundWrapper
          .eq(Order::getPackageId, request.packageId())
          .eq(Order::getType, OrderType.REFUND.getValue())
          .eq(Order::getStatus, OrderStatus.REFUND_PENDING.getValue());
      if (orderMapper.selectCount(pendingRefundWrapper) > 0) {
        throw new BusinessException(ErrorCode.REFUND_PENDING_EXISTS);
      }

      LambdaQueryWrapper<Order> purchaseWrapper = new LambdaQueryWrapper<>();
      purchaseWrapper
          .eq(Order::getPackageId, request.packageId())
          .eq(Order::getType, OrderType.PURCHASE.getValue())
          .eq(Order::getStatus, OrderStatus.PAID.getValue())
          .orderByDesc(Order::getId)
          .last("LIMIT 1");
      Order purchaseOrder = orderMapper.selectOne(purchaseWrapper);
      if (purchaseOrder == null) {
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "未找到对应购买订单");
      }

      BigDecimal calculatedAmount = packageService.calculateRefundAmount(coursePackage);
      BigDecimal refundAmount =
          request.refundAmount() == null
              ? calculatedAmount
              : request.refundAmount().setScale(2, RoundingMode.HALF_UP);
      if (refundAmount.compareTo(BigDecimal.ZERO) < 0
          || refundAmount.compareTo(calculatedAmount) > 0) {
        throw new BusinessException(
            ErrorCode.REFUND_AMOUNT_INVALID,
            "退款金额必须在 0 到 " + calculatedAmount + " 之间");
      }
      if (request.refundAmount() != null
          && refundAmount.compareTo(calculatedAmount) != 0
          && StringUtils.isBlank(request.adjustReason())) {
        throw new BusinessException(ErrorCode.REFUND_AMOUNT_INVALID, "修改退款金额需填写调整原因");
      }
      if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "可退金额必须大于 0");
      }

      Order refundOrder = new Order();
      refundOrder.setOrderNo(OrderNoGenerator.generateOrderNo(REFUND_ORDER_PREFIX));
      refundOrder.setType(OrderType.REFUND.getValue());
      refundOrder.setStatus(OrderStatus.REFUND_PENDING.getValue());
      refundOrder.setUserId(coursePackage.getUserId());
      refundOrder.setCoachId(coursePackage.getCoachId());
      refundOrder.setPackageId(coursePackage.getId());
      refundOrder.setPurchaseOrderId(purchaseOrder.getId());
      refundOrder.setOriginalAmount(refundAmount);
      refundOrder.setPaidAmount(refundAmount);
      refundOrder.setReason(request.reason());
      orderMapper.insert(refundOrder);

      RefundRecord refundRecord = new RefundRecord();
      refundRecord.setPackageId(coursePackage.getId());
      refundRecord.setOrderId(refundOrder.getId());
      refundRecord.setRefundAmount(refundAmount);
      refundRecord.setReason(request.reason());
      refundRecord.setStatus(REFUND_RECORD_STATUS_PENDING);
      refundRecordMapper.insert(refundRecord);

      CoursePackage updatePackage = new CoursePackage();
      updatePackage.setId(coursePackage.getId());
      updatePackage.setVersion(request.version());
      updatePackage.setStatus(STATUS_FROZEN);
      updatePackage.setFrozenReason(REFUND_FROZEN_REASON);

      int affected = packageMapper.updateById(updatePackage);
      if (affected == 0) {
        throw new BusinessException(ErrorCode.PACKAGE_CONCURRENTLY_UPDATED);
      }

      writeAuditLog(
          adminId,
          coursePackage.getId(),
          ACTION_REQUEST_REFUND,
          snapshot(coursePackage),
          snapshot(updatePackage),
          request.reason(),
          null);

      log.info(
          "Admin request package refund: adminId={}, packageId={}, orderId={}, amount={}",
          adminId,
          coursePackage.getId(),
          refundOrder.getId(),
          refundAmount);

      return AdminPackageOperationResponse.of(
          "退款订单已生成，请前往订单管理审批", refundOrder.getOrderNo());
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  private CoursePackage findPackage(Long packageId) {
    CoursePackage coursePackage = packageMapper.selectById(packageId);
    if (coursePackage == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }
    return coursePackage;
  }

  private void validateVersion(CoursePackage coursePackage, Integer requestVersion) {
    if (!Objects.equals(coursePackage.getVersion(), requestVersion)) {
      throw new BusinessException(ErrorCode.PACKAGE_CONCURRENTLY_UPDATED);
    }
  }

  private Map<Long, User> findUserMap(List<CoursePackage> records) {
    return userMapper
        .selectBatchIds(records.stream().map(CoursePackage::getUserId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
  }

  private Map<Long, Coach> findCoachMap(List<CoursePackage> records) {
    return coachMapper
        .selectBatchIds(
            records.stream()
                .map(CoursePackage::getCoachId)
                .filter(Objects::nonNull)
                .distinct()
                .toList())
        .stream()
        .collect(Collectors.toMap(Coach::getId, Function.identity(), (a, b) -> a));
  }

  private int releaseReservedAndCancelBookings(CoursePackage coursePackage, LocalDateTime now) {
    int released =
        bookingMapper.cancelFutureByPackageId(coursePackage.getId(), now);
    log.info(
        "Released reserved bookings on freeze: packageId={}, count={}",
        coursePackage.getId(),
        released);
    return released;
  }

  private List<AdminPackageDetailResponse.ConsumptionRecord> buildConsumptionRecords(Long packageId) {
    LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Booking::getPackageId, packageId);
    wrapper.orderByDesc(Booking::getStartTime);
    List<Booking> bookings = bookingMapper.selectList(wrapper);

    List<AdminPackageDetailResponse.ConsumptionRecord> records = new ArrayList<>();
    for (Booking booking : bookings) {
      int hours =
          booking.getEndTime() == null || booking.getStartTime() == null
              ? 0
              : (int) java.time.Duration.between(booking.getStartTime(), booking.getEndTime()).toHours();
      records.add(
          new AdminPackageDetailResponse.ConsumptionRecord(
              booking.getId(),
              booking.getStartTime(),
              booking.getEndTime(),
              booking.getStatus(),
              hours,
              booking.getCreatedAt()));
    }
    return records;
  }

  private List<AdminPackageDetailResponse.OrderInfo> buildRelatedOrders(Long packageId) {
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Order::getPackageId, packageId).orderByDesc(Order::getCreatedAt);
    List<Order> orders = orderMapper.selectList(wrapper);
    return orders.stream()
        .map(
            order ->
                new AdminPackageDetailResponse.OrderInfo(
                    order.getId(),
                    order.getOrderNo(),
                    order.getType(),
                    order.getStatus(),
                    order.getPaidAmount(),
                    order.getCreatedAt()))
        .toList();
  }

  private void writeAuditLog(
      Long adminId,
      Long packageId,
      String action,
      String beforeSnapshot,
      String afterSnapshot,
      String reason,
      String ip) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActorType(ACTOR_TYPE);
    auditLog.setActorId(adminId);
    auditLog.setTargetType(TARGET_TYPE);
    auditLog.setTargetId(packageId);
    auditLog.setAction(action);
    auditLog.setBeforeSnapshot(beforeSnapshot);
    auditLog.setAfterSnapshot(afterSnapshot);
    auditLog.setReason(reason);
    auditLog.setIp(ip);
    auditLog.setCreatedAt(LocalDateTime.now());
    auditLogMapper.insert(auditLog);
  }

  private String snapshot(CoursePackage coursePackage) {
    return String.format(
        "status=%s, frozenReason=%s, reserved=%s, available=%s, expireAt=%s, extendReason=%s",
        coursePackage.getStatus(),
        coursePackage.getFrozenReason(),
        coursePackage.getReservedCount(),
        coursePackage.getAvailableCount(),
        coursePackage.getExpireAt(),
        coursePackage.getExtendReason());
  }
}
