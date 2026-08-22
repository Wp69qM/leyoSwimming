package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.UserPackageDetailRequest;
import com.leyoswimming.dto.request.UserPackageListRequest;
import com.leyoswimming.dto.request.UserPackageRefundRequest;
import com.leyoswimming.dto.response.UserActivePackageResponse;
import com.leyoswimming.dto.response.UserPackageDetailResponse;
import com.leyoswimming.dto.response.UserPackageListItemResponse;
import com.leyoswimming.dto.response.UserPackageQualificationResponse;
import com.leyoswimming.dto.response.UserPackageRefundResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.RefundRecord;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.RefundRecordMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

  private static final String REFUND_ORDER_PREFIX = "R";
  private static final String REFUND_FROZEN_REASON = "refund_pending";
  private static final String COACH_RESIGNED_FROZEN_REASON = "coach_resigned";
  private static final String LOCK_RESOURCE_REFUND_APPLY = "refund:apply";
  private static final Duration LOCK_TTL_REFUND_APPLY = Duration.ofSeconds(10);

  private final PackageMapper packageMapper;
  private final OrderMapper orderMapper;
  private final RefundRecordMapper refundRecordMapper;
  private final DistributedLockHelper lockHelper;

  @Transactional
  public UserPackageRefundResponse requestRefund(Long userId, UserPackageRefundRequest request) {
    CoursePackage coursePackage = packageMapper.selectById(request.packageId());
    if (coursePackage == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }
    if (!userId.equals(coursePackage.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!isRefundStatusAllowed(coursePackage)) {
      throw new BusinessException(ErrorCode.PACKAGE_STATUS_NOT_ALLOWED, "当前套餐状态不允许申请退款");
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
      if (hasPendingRefundOrder(request.packageId())) {
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

      BigDecimal refundAmount = calculateRefundAmount(coursePackage);
      if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_REFUNDABLE, "可退金额必须大于 0");
      }

      Order refundOrder = new Order();
      refundOrder.setOrderNo(OrderNoGenerator.generateOrderNo(REFUND_ORDER_PREFIX));
      refundOrder.setType(OrderType.REFUND.getValue());
      refundOrder.setStatus(OrderStatus.REFUND_PENDING.getValue());
      refundOrder.setUserId(userId);
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
      refundRecord.setStatus(0);
      refundRecordMapper.insert(refundRecord);

      CoursePackage updatePackage = new CoursePackage();
      updatePackage.setId(coursePackage.getId());
      updatePackage.setStatus("frozen");
      updatePackage.setFrozenReason(REFUND_FROZEN_REASON);
      updatePackage.setVersion(coursePackage.getVersion());
      packageMapper.updateById(updatePackage);

      log.info(
          "Refund requested: userId={}, packageId={}, orderId={}, amount={}",
          userId,
          coursePackage.getId(),
          refundOrder.getId(),
          refundAmount);

      return new UserPackageRefundResponse(
          refundOrder.getId(), refundOrder.getOrderNo(), refundAmount);
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  public UserActivePackageResponse getActivePackage(Long userId) {
    CoursePackage coursePackage = packageMapper.findFirstActiveByUserId(userId);
    if (coursePackage == null) {
      return null;
    }
    return new UserActivePackageResponse(
        coursePackage.getId(),
        coursePackage.getCoachId(),
        coursePackage.getCoachName(),
        coursePackage.getStatus());
  }

  @Transactional(readOnly = true)
  public UserPackageQualificationResponse getPackageQualification(Long userId) {
    CoursePackage activePackage = packageMapper.findFirstActiveByUserId(userId);
    CoursePackage experiencePackage = packageMapper.findExperiencePackageByUserId(userId);
    return new UserPackageQualificationResponse(
        activePackage != null,
        activePackage != null ? activePackage.getId() : null,
        activePackage != null ? activePackage.getCoachId() : null,
        experiencePackage != null);
  }

  @Transactional(readOnly = true)
  public UserPackageDetailResponse detail(Long userId, UserPackageDetailRequest request) {
    CoursePackage coursePackage = packageMapper.selectById(request.packageId());
    if (coursePackage == null) {
      throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND);
    }
    if (!userId.equals(coursePackage.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return toDetailResponse(coursePackage);
  }

  @Transactional(readOnly = true)
  public List<UserPackageListItemResponse> list(Long userId, UserPackageListRequest request) {
    LambdaQueryWrapper<CoursePackage> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(CoursePackage::getUserId, userId);
    if (StringUtils.isNotBlank(request.status())) {
      wrapper.eq(CoursePackage::getStatus, request.status());
    }
    wrapper.orderByDesc(CoursePackage::getCreatedAt);

    return packageMapper.selectList(wrapper).stream()
        .map(this::toListItemResponse)
        .toList();
  }

  private UserPackageDetailResponse toDetailResponse(CoursePackage coursePackage) {
    return new UserPackageDetailResponse(
        coursePackage.getId(),
        coursePackage.getPackageName(),
        coursePackage.getPackageMode(),
        coursePackage.getCoachName(),
        coursePackage.getTeachingType(),
        coursePackage.getStrokeIds(),
        coursePackage.getDurationMinutes(),
        coursePackage.getValidDays(),
        coursePackage.getTotalHours(),
        coursePackage.getConsumedCount(),
        coursePackage.getAvailableCount(),
        coursePackage.getPaidAmount(),
        coursePackage.getOriginalPrice(),
        coursePackage.getRefundEnabled(),
        coursePackage.getRefundValidDays(),
        coursePackage.getRefundRatio(),
        buildRefundRuleText(coursePackage),
        coursePackage.getStatus(),
        coursePackage.getFrozenReason(),
        coursePackage.getExpireAt(),
        coursePackage.getCreatedAt(),
        canRefund(coursePackage));
  }

  private String buildRefundRuleText(CoursePackage coursePackage) {
    if (Boolean.FALSE.equals(coursePackage.getRefundEnabled())) {
      return "不支持退款";
    }
    if ("frozen".equals(coursePackage.getStatus())
        && "coach_resigned".equals(coursePackage.getFrozenReason())) {
      return "教练离职，可申请 100% 全额退款";
    }
    if (coursePackage.getRefundValidDays() == null
        || coursePackage.getRefundValidDays() <= 0
        || coursePackage.getRefundRatio() == null) {
      return "按套餐快照规则退款";
    }
    return String.format(
        "开课后 %d 天内可申请退款，退款比例 %d%%",
        coursePackage.getRefundValidDays(),
        coursePackage
            .getRefundRatio()
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue());
  }

  private UserPackageListItemResponse toListItemResponse(CoursePackage coursePackage) {
    return new UserPackageListItemResponse(
        coursePackage.getId(),
        coursePackage.getPackageName(),
        coursePackage.getPackageMode(),
        coursePackage.getCoachName(),
        coursePackage.getTeachingType(),
        coursePackage.getDurationMinutes(),
        coursePackage.getValidDays(),
        coursePackage.getTotalHours(),
        coursePackage.getConsumedCount(),
        coursePackage.getAvailableCount(),
        coursePackage.getPaidAmount(),
        coursePackage.getOriginalPrice(),
        coursePackage.getStatus(),
        coursePackage.getFrozenReason(),
        coursePackage.getRefundEnabled(),
        coursePackage.getRefundValidDays(),
        coursePackage.getRefundRatio(),
        coursePackage.getExpireAt(),
        coursePackage.getCreatedAt(),
        canRefund(coursePackage));
  }

  private boolean canRefund(CoursePackage coursePackage) {
    if (Boolean.FALSE.equals(coursePackage.getRefundEnabled())) {
      return false;
    }
    if (hasPendingRefundOrder(coursePackage.getId())) {
      return false;
    }
    if (!isRefundStatusAllowed(coursePackage)) {
      return false;
    }
    if (coursePackage.getRefundValidDays() != null
        && coursePackage.getRefundValidDays() > 0
        && coursePackage.getCreatedAt() != null) {
      LocalDateTime deadline =
          coursePackage.getCreatedAt().plusDays(coursePackage.getRefundValidDays());
      if (LocalDateTime.now().isAfter(deadline)) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPendingRefundOrder(Long packageId) {
    LambdaQueryWrapper<Order> pendingRefundWrapper = new LambdaQueryWrapper<>();
    pendingRefundWrapper
        .eq(Order::getPackageId, packageId)
        .eq(Order::getType, OrderType.REFUND.getValue())
        .eq(Order::getStatus, OrderStatus.REFUND_PENDING.getValue());
    return orderMapper.selectCount(pendingRefundWrapper) > 0;
  }

  public BigDecimal calculateRefundAmount(CoursePackage coursePackage) {
    BigDecimal paidAmount =
        coursePackage.getPaidAmount() == null ? BigDecimal.ZERO : coursePackage.getPaidAmount();

    if (COACH_RESIGNED_FROZEN_REASON.equals(coursePackage.getFrozenReason())) {
      return paidAmount.setScale(2, RoundingMode.HALF_UP);
    }

    int totalHours =
        coursePackage.getTotalHours() == null ? 0 : coursePackage.getTotalHours();
    int consumedCount =
        coursePackage.getConsumedCount() == null ? 0 : coursePackage.getConsumedCount();
    BigDecimal refundRatio =
        coursePackage.getRefundRatio() == null
            ? BigDecimal.ONE
            : coursePackage.getRefundRatio();

    if (totalHours <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal remainingRatio =
        BigDecimal.valueOf(totalHours - consumedCount)
            .divide(BigDecimal.valueOf(totalHours), 4, RoundingMode.HALF_UP);
    return paidAmount
        .multiply(remainingRatio)
        .multiply(refundRatio)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private boolean isRefundStatusAllowed(CoursePackage coursePackage) {
    String status = coursePackage.getStatus();
    return "active".equals(status)
        || "expired".equals(status)
        || ("frozen".equals(status)
            && COACH_RESIGNED_FROZEN_REASON.equals(coursePackage.getFrozenReason()));
  }
}
