package com.leyoswimming.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.UserCancelCheckResponse;
import com.leyoswimming.dto.response.UserCancelResponse;
import com.leyoswimming.entity.User;
import com.leyoswimming.entity.UserSession;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.repository.UserSessionMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountService {

  private static final int ANONYMOUS_DAYS = 90;

  private final UserMapper userMapper;
  private final UserSessionMapper userSessionMapper;
  private final PackageMapper packageMapper;

  public UserCancelCheckResponse cancelCheck(Long userId) {
    User user =
        userMapper
            .selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    if (user.getStatus() == UserStatus.DELETED.getValue()) {
      throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
    }

    // TODO(US-020/US-025): 真实查询 active 套餐、未完成订单、进行中预约
    boolean noActivePackage = packageMapper.findFirstActiveByUserId(userId) == null;
    boolean noPendingOrder = true;
    boolean noOngoingBooking = true;

    boolean canCancel = noActivePackage && noPendingOrder && noOngoingBooking;
    return new UserCancelCheckResponse(
        canCancel,
        new UserCancelCheckResponse.CancelChecks(noActivePackage, noPendingOrder, noOngoingBooking));
  }

  @Transactional
  public UserCancelResponse cancel(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    if (user.getStatus() == UserStatus.DELETED.getValue()) {
      return new UserCancelResponse(true, user.getAnonymousAfter());
    }

    UserCancelCheckResponse check = cancelCheck(userId);
    if (!check.canCancel()) {
      if (!check.checks().noActivePackage()) {
        throw new BusinessException(ErrorCode.ACTIVE_PACKAGE_EXISTS);
      }
      if (!check.checks().noPendingOrder()) {
        throw new BusinessException(ErrorCode.PENDING_ORDER_EXISTS);
      }
      throw new BusinessException(ErrorCode.ONGOING_BOOKING_EXISTS);
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime anonymousAfter = now.plusDays(ANONYMOUS_DAYS);
    user.setStatus(UserStatus.DELETED.getValue());
    user.setDeletedAt(now);
    user.setAnonymousAfter(anonymousAfter);
    userMapper.updateById(user);

    userSessionMapper.delete(
        new LambdaQueryWrapper<UserSession>().eq(UserSession::getUserId, userId));

    log.info("User account cancelled: userId={}, anonymousAfter={}", userId, anonymousAfter);
    return new UserCancelResponse(true, anonymousAfter);
  }
}
