package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.request.OrderFormalRequest;
import com.leyoswimming.dto.request.OrderListRequest;
import com.leyoswimming.dto.request.OrderTrialRequest;
import com.leyoswimming.dto.response.OrderCreateResponse;
import com.leyoswimming.dto.response.OrderDetailResponse;
import com.leyoswimming.dto.response.OrderListResponse;
import com.leyoswimming.entity.Coach;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.CustomPackageConfig;
import com.leyoswimming.entity.Order;
import com.leyoswimming.entity.PackageTemplate;
import com.leyoswimming.entity.PackageTemplateCoach;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.CoachStatus;
import com.leyoswimming.enums.OrderStatus;
import com.leyoswimming.enums.OrderType;
import com.leyoswimming.enums.PackageMode;
import com.leyoswimming.enums.PackageTemplateStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.CoachMapper;
import com.leyoswimming.repository.CustomPackageConfigMapper;
import com.leyoswimming.repository.OrderMapper;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.PackageTemplateCoachMapper;
import com.leyoswimming.repository.PackageTemplateMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.service.DistributedLockHelper.LockToken;
import com.leyoswimming.util.OrderNoGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderService {

  private static final String PURCHASE_ORDER_PREFIX = "O";
  private static final int DEFAULT_CUSTOM_HOURS_MIN = 1;
  private static final int DEFAULT_CUSTOM_HOURS_MAX = 50;
  private static final int DEFAULT_CUSTOM_VALID_DAYS = 30;
  private static final int CUSTOM_VALID_DAYS_MAX = 365;
  private static final int ORDER_EXPIRE_HOURS = 24;
  private static final String LOCK_RESOURCE_ORDER_CREATE = "order:create";
  private static final Duration LOCK_TTL_ORDER_CREATE = Duration.ofSeconds(30);
  private static final int TRIAL_DURATION_MINUTES = 60;
  private static final int TRIAL_VALID_DAYS = 30;
  private static final int TRIAL_TOTAL_HOURS = 1;
  private static final String PACKAGE_NAME_TRIAL = "体验课";
  private static final String PACKAGE_NAME_CUSTOM = "自定义套餐";
  private static final String TEACHING_TYPE_ONE_ON_ONE = "one_on_one";
  private static final String COURSE_PACKAGE_STATUS_ACTIVE = "active";
  private static final String COURSE_PACKAGE_STATUS_EXHAUSTED = "exhausted";

  private final OrderMapper orderMapper;
  private final PackageMapper packageMapper;
  private final UserMapper userMapper;
  private final CoachMapper coachMapper;
  private final PackageTemplateMapper packageTemplateMapper;
  private final PackageTemplateCoachMapper packageTemplateCoachMapper;
  private final CustomPackageConfigMapper customPackageConfigMapper;
  private final DistributedLockHelper lockHelper;

  @Transactional
  public OrderCreateResponse createTrialOrder(Long userId, OrderTrialRequest request) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    Coach coach = coachMapper.selectById(request.coachId());
    if (coach == null || !isPublicCoachStatus(coach.getStatus())) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }

    LockToken lock = lockHelper.lock(LOCK_RESOURCE_ORDER_CREATE, "user:" + userId, LOCK_TTL_ORDER_CREATE);
    try {
      ensureNoActivePackage(userId);

      LambdaQueryWrapper<CoursePackage> packageWrapper = new LambdaQueryWrapper<>();
      packageWrapper
          .eq(CoursePackage::getUserId, userId)
          .eq(CoursePackage::getPackageMode, PackageMode.EXPERIENCE.getValue())
          .in(CoursePackage::getStatus, List.of(COURSE_PACKAGE_STATUS_ACTIVE, COURSE_PACKAGE_STATUS_EXHAUSTED));
      if (packageMapper.selectCount(packageWrapper) > 0) {
        throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "您已购买过体验课，无法重复购买");
      }

      Order order = buildPurchaseOrder(user, coach, null, null);
      order.setPackageMode(PackageMode.EXPERIENCE.getValue());
      order.setPackageName(PACKAGE_NAME_TRIAL);
      order.setCoachName(coach.getName());
      order.setTeachingType(TEACHING_TYPE_ONE_ON_ONE);
      order.setTotalHours(TRIAL_TOTAL_HOURS);
      order.setDurationMinutes(TRIAL_DURATION_MINUTES);
      order.setValidDays(TRIAL_VALID_DAYS);
      order.setRefundEnabled(Boolean.FALSE);
      order.setRefundRatio(BigDecimal.ZERO);
      order.setRefundValidDays(0);
      order.setOriginalAmount(BigDecimal.ZERO);
      order.setPaidAmount(BigDecimal.ZERO);
      orderMapper.insert(order);

      log.info("Trial order created: userId={}, orderId={}, coachId={}", userId, order.getId(), coach.getId());
      return new OrderCreateResponse(order.getId(), order.getOrderNo(), order.getOriginalAmount(), order.getExpireAt());
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  @Transactional
  public OrderCreateResponse createFormalOrder(Long userId, OrderFormalRequest request) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    Coach coach = coachMapper.selectById(request.coachId());
    if (coach == null || !isPublicCoachStatus(coach.getStatus())) {
      throw new BusinessException(ErrorCode.COACH_NOT_FOUND);
    }

    PackageTemplate template = packageTemplateMapper.selectById(request.packageId());
    if (template == null
        || !PackageTemplateStatus.ACTIVE.getValue().equals(template.getStatus())) {
      throw new BusinessException(ErrorCode.PACKAGE_TEMPLATE_NOT_FOUND);
    }

    boolean isCustom = PackageMode.CUSTOM.getValue().equals(template.getPackageMode());
    if (!isCustom && !PackageMode.STANDARD.getValue().equals(template.getPackageMode())) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "不支持的套餐模式");
    }

    if (!isCustom) {
      LambdaQueryWrapper<PackageTemplateCoach> linkWrapper = new LambdaQueryWrapper<>();
      linkWrapper
          .eq(PackageTemplateCoach::getPackageTemplateId, template.getId())
          .eq(PackageTemplateCoach::getCoachId, coach.getId());
      if (packageTemplateCoachMapper.selectCount(linkWrapper) == 0) {
        throw new BusinessException(ErrorCode.PACKAGE_NOT_FOUND, "该教练不适用此套餐");
      }
    }

    LockToken lock = lockHelper.lock(LOCK_RESOURCE_ORDER_CREATE, "user:" + userId, LOCK_TTL_ORDER_CREATE);
    try {
      ensureNoActivePackage(userId);

      validateFormalTemplate(template, isCustom);
      OrderPriceResult priceResult = calculateFormalPrice(template, coach, request, isCustom);

      Order order = buildPurchaseOrder(user, coach, template, priceResult.originalAmount());
      order.setPackageTemplateId(template.getId());
      order.setPackageMode(isCustom ? PackageMode.CUSTOM.getValue() : PackageMode.STANDARD.getValue());
      order.setPackageName(isCustom ? PACKAGE_NAME_CUSTOM : template.getName());
      order.setCoachName(coach.getName());
      order.setTeachingType(template.getTeachingType());
      order.setStrokeIds(request.strokeIds());
      order.setTotalHours(priceResult.hours());
      order.setDurationMinutes(template.getDurationMinutes());
      order.setValidDays(priceResult.validDays());
      order.setRefundEnabled(template.getRefundEnabled());
      order.setRefundRatio(template.getRefundRatio());
      order.setRefundValidDays(template.getRefundValidDays());
      orderMapper.insert(order);

      log.info("Formal order created: userId={}, orderId={}, templateId={}", userId, order.getId(), template.getId());
      return new OrderCreateResponse(order.getId(), order.getOrderNo(), order.getOriginalAmount(), order.getExpireAt());
    } finally {
      lockHelper.unlockAfterTransaction(lock);
    }
  }

  @Transactional(readOnly = true)
  public OrderDetailResponse detail(Long userId, Long orderId) {
    Order order = orderMapper.selectById(orderId);
    if (order == null || !userId.equals(order.getUserId())) {
      throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }
    return toDetailResponse(order);
  }

  @Transactional(readOnly = true)
  public Page<OrderListResponse> list(Long userId, OrderListRequest request) {
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Order::getUserId, userId);
    applyTabFilter(wrapper, request.tab());
    wrapper.orderByDesc(Order::getCreatedAt);

    Page<Order> page = new Page<>(request.page(), request.size());
    Page<Order> result = orderMapper.selectPage(page, wrapper);

    List<OrderListResponse> records =
        result.getRecords().stream().map(this::toListResponse).toList();
    Page<OrderListResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
    responsePage.setRecords(records);
    return responsePage;
  }

  private void applyTabFilter(LambdaQueryWrapper<Order> wrapper, String tab) {
    if (StringUtils.isBlank(tab) || "all".equalsIgnoreCase(tab)) {
      return;
    }
    switch (tab.toLowerCase()) {
      case "pending_payment" -> wrapper.eq(Order::getStatus, "pending_payment");
      case "completed" -> wrapper.in(Order::getStatus, List.of("paid", "refunded"));
      case "refunding" ->
          wrapper.in(Order::getStatus, List.of("refund_pending", "dispute_processing", "refund_processing"));
      case "cancelled" -> wrapper.eq(Order::getStatus, "cancelled");
      default -> wrapper.eq(Order::getStatus, tab.toLowerCase());
    }
  }

  private OrderListResponse toListResponse(Order order) {
    return new OrderListResponse(
        order.getId(),
        order.getOrderNo(),
        order.getStatus(),
        order.getOriginalAmount(),
        order.getPaidAmount(),
        order.getExpireAt(),
        order.getPackageName(),
        order.getPackageMode(),
        order.getCoachName(),
        order.getTeachingType(),
        order.getTotalHours(),
        order.getCreatedAt());
  }

  private Order buildPurchaseOrder(User user, Coach coach, PackageTemplate template, BigDecimal amount) {
    Order order = new Order();
    order.setOrderNo(OrderNoGenerator.generateOrderNo(PURCHASE_ORDER_PREFIX));
    order.setType(OrderType.PURCHASE.getValue());
    order.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
    order.setUserId(user.getId());
    order.setCoachId(coach == null ? null : coach.getId());
    order.setOriginalAmount(amount == null ? BigDecimal.ZERO : amount);
    order.setPaidAmount(BigDecimal.ZERO);
    order.setExpireAt(LocalDateTime.now().plusHours(ORDER_EXPIRE_HOURS));
    return order;
  }

  private void ensureNoActivePackage(Long userId) {
    LambdaQueryWrapper<CoursePackage> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(CoursePackage::getUserId, userId)
        .eq(CoursePackage::getStatus, COURSE_PACKAGE_STATUS_ACTIVE);
    if (packageMapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.INVALID_PACKAGE_PARAM, "您已拥有 active 套餐，请先完成或退款后再购买");
    }
  }

  private boolean isPublicCoachStatus(Integer status) {
    return status != null
        && (status == CoachStatus.APPROVED.getValue()
            || status == CoachStatus.RESIGNING.getValue());
  }

  private void validateFormalTemplate(PackageTemplate template, boolean isCustom) {
    if (isCustom) {
      return;
    }
    if (template.getPrice() == null
        || template.getPrice().compareTo(BigDecimal.ZERO) <= 0
        || template.getTotalHours() == null
        || template.getTotalHours() <= 0) {
      throw new BusinessException(
          ErrorCode.INVALID_PACKAGE_PARAM, "套餐模板价格或课时配置无效");
    }
  }

  private OrderPriceResult calculateFormalPrice(
      PackageTemplate template, Coach coach, OrderFormalRequest request, boolean isCustom) {
    if (isCustom) {
      CustomPackageConfig config = customPackageConfigMapper.findFirst();
      int minHours = config != null && config.getMinHours() != null
          ? config.getMinHours() : DEFAULT_CUSTOM_HOURS_MIN;
      int maxHours = config != null && config.getMaxHours() != null
          ? config.getMaxHours() : DEFAULT_CUSTOM_HOURS_MAX;
      if (request.hours() == null || request.hours() < minHours || request.hours() > maxHours) {
        throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG,
            "自定义课时需在 " + minHours + "-" + maxHours + " 之间");
      }
      if (coach.getReferencePrice() == null
          || coach.getReferencePrice().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG, "该教练未设置参考单价");
      }
      int validDays = request.validDays() != null ? request.validDays() : DEFAULT_CUSTOM_VALID_DAYS;
      if (validDays > CUSTOM_VALID_DAYS_MAX) {
        throw new BusinessException(ErrorCode.INVALID_CUSTOM_PACKAGE_CONFIG,
            "自定义套餐有效期不能超过 " + CUSTOM_VALID_DAYS_MAX + " 天");
      }
      BigDecimal pricePerHour = coach.getReferencePrice();
      BigDecimal originalAmount = pricePerHour.multiply(BigDecimal.valueOf(request.hours()));
      return new OrderPriceResult(request.hours(), validDays, pricePerHour, originalAmount);
    }
    int hours = template.getTotalHours();
    int validDays = template.getValidDays();
    BigDecimal pricePerHour = template.getPrice().divide(BigDecimal.valueOf(hours), 2, RoundingMode.HALF_UP);
    BigDecimal originalAmount = template.getPrice();
    return new OrderPriceResult(hours, validDays, pricePerHour, originalAmount);
  }

  private record OrderPriceResult(
      int hours, int validDays, BigDecimal pricePerHour, BigDecimal originalAmount) {
  }

  private OrderDetailResponse toDetailResponse(Order order) {
    return new OrderDetailResponse(
        order.getId(),
        order.getOrderNo(),
        order.getType(),
        order.getStatus(),
        order.getOriginalAmount(),
        order.getPaidAmount(),
        order.getPaidAt(),
        order.getExpireAt(),
        order.getPackageId(),
        order.getPackageName(),
        order.getPackageMode(),
        order.getOriginalAmount(),
        order.getCoachName(),
        order.getTeachingType(),
        order.getTotalHours(),
        order.getDurationMinutes(),
        order.getValidDays(),
        order.getCreatedAt());
  }
}
