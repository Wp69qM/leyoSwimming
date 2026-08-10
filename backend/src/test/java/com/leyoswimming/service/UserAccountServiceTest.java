package com.leyoswimming.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.dto.response.UserCancelCheckResponse;
import com.leyoswimming.dto.response.UserCancelResponse;
import com.leyoswimming.entity.CoursePackage;
import com.leyoswimming.entity.User;
import com.leyoswimming.enums.UserStatus;
import com.leyoswimming.exception.BusinessException;
import com.leyoswimming.repository.PackageMapper;
import com.leyoswimming.repository.UserMapper;
import com.leyoswimming.repository.UserSessionMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private UserMapper userMapper;
  @Mock private UserSessionMapper userSessionMapper;
  @Mock private PackageMapper packageMapper;

  private UserAccountService userAccountService;

  @BeforeEach
  void setUp() {
    userAccountService = new UserAccountService(userMapper, userSessionMapper, packageMapper);
  }

  @Test
  @DisplayName("注销检查：正常用户且无活跃套餐，返回可以注销")
  void cancelCheck_activeUserWithoutPackage_canCancel() {
    when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
    when(packageMapper.findFirstActiveByUserId(1L)).thenReturn(null);

    UserCancelCheckResponse response = userAccountService.cancelCheck(1L);

    assertThat(response.canCancel()).isTrue();
    assertThat(response.checks().noActivePackage()).isTrue();
    assertThat(response.checks().noPendingOrder()).isTrue();
    assertThat(response.checks().noOngoingBooking()).isTrue();
  }

  @Test
  @DisplayName("注销检查：存在活跃套餐，返回不可注销")
  void cancelCheck_activeUserWithPackage_cannotCancelDueToPackage() {
    when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
    when(packageMapper.findFirstActiveByUserId(1L)).thenReturn(new CoursePackage());

    UserCancelCheckResponse response = userAccountService.cancelCheck(1L);

    assertThat(response.canCancel()).isFalse();
    assertThat(response.checks().noActivePackage()).isFalse();
    assertThat(response.checks().noPendingOrder()).isTrue();
    assertThat(response.checks().noOngoingBooking()).isTrue();
  }

  @Test
  @DisplayName("注销检查：用户不存在抛出 USER_NOT_FOUND")
  void cancelCheck_userNotFound_throwsUserNotFound() {
    when(userMapper.selectById(1L)).thenReturn(null);

    assertThatThrownBy(() -> userAccountService.cancelCheck(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND));
  }

  @Test
  @DisplayName("注销检查：已注销用户抛出 USER_ALREADY_DELETED")
  void cancelCheck_alreadyDeletedUser_throwsUserAlreadyDeleted() {
    User user = activeUser(1L);
    user.setStatus(UserStatus.DELETED.getValue());
    when(userMapper.selectById(1L)).thenReturn(user);

    assertThatThrownBy(() -> userAccountService.cancelCheck(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_ALREADY_DELETED));

    verify(packageMapper, never()).findFirstActiveByUserId(any());
  }

  @Test
  @DisplayName("账号注销：正常用户成功注销并清除会话")
  void cancel_activeUser_success() {
    User user = activeUser(1L);
    when(userMapper.selectById(1L)).thenReturn(user);
    when(packageMapper.findFirstActiveByUserId(1L)).thenReturn(null);

    UserCancelResponse response = userAccountService.cancel(1L);

    assertThat(response.cancelled()).isTrue();
    assertThat(response.anonymousAfter()).isAfter(LocalDateTime.now().plusDays(89));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).updateById(userCaptor.capture());
    User updated = userCaptor.getValue();
    assertThat(updated.getStatus()).isEqualTo(UserStatus.DELETED.getValue());
    assertThat(updated.getDeletedAt()).isNotNull();
    assertThat(updated.getAnonymousAfter()).isEqualTo(response.anonymousAfter());

    verify(userSessionMapper).delete(any());
  }

  @Test
  @DisplayName("账号注销：存在活跃套餐抛出 ACTIVE_PACKAGE_EXISTS")
  void cancel_activeUserWithPackage_throwsActivePackageExists() {
    User user = activeUser(1L);
    when(userMapper.selectById(1L)).thenReturn(user);
    when(packageMapper.findFirstActiveByUserId(1L)).thenReturn(new CoursePackage());

    assertThatThrownBy(() -> userAccountService.cancel(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ACTIVE_PACKAGE_EXISTS));

    verify(userMapper, never()).updateById(any(User.class));
    verify(userSessionMapper, never()).delete(any());
  }

  @Test
  @DisplayName("账号注销：已注销用户再次调用返回幂等结果")
  void cancel_alreadyDeletedUser_returnsExistingAnonymousAfter() {
    User user = activeUser(1L);
    user.setStatus(UserStatus.DELETED.getValue());
    LocalDateTime anonymousAfter = LocalDateTime.now().plusDays(60);
    user.setAnonymousAfter(anonymousAfter);
    when(userMapper.selectById(1L)).thenReturn(user);

    UserCancelResponse response = userAccountService.cancel(1L);

    assertThat(response.cancelled()).isTrue();
    assertThat(response.anonymousAfter()).isEqualTo(anonymousAfter);

    verify(userMapper, never()).updateById(any(User.class));
    verify(userSessionMapper, never()).delete(any());
  }

  private User activeUser(Long id) {
    User user = new User();
    user.setId(id);
    user.setOpenid("openid_" + id);
    user.setPhone("phone_" + id);
    user.setStatus(UserStatus.ACTIVE.getValue());
    return user;
  }
}
