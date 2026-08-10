package com.leyoswimming.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            86400000L);
  }

  @Test
  @DisplayName("生成管理员 token 并解析用户 ID")
  void generateAdminToken_parse_returnsAdminUserId() {
    String token = jwtTokenProvider.generateAdminToken(1L, "admin");

    assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    assertThat(jwtTokenProvider.getAdminUserId(token)).isEqualTo(1L);
    assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("admin");
  }

  @Test
  @DisplayName("生成用户 token 并解析用户 ID 与资料完成状态")
  void generateUserAccessToken_parse_returnsUserClaims() {
    String token = jwtTokenProvider.generateUserAccessToken(2L, true);

    assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("user");
    assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(2L);
    assertThat(jwtTokenProvider.isProfileCompleted(token)).isTrue();
  }

  @Test
  @DisplayName("生成教练 token 并解析教练 ID 与状态")
  void generateCoachAccessToken_parse_returnsCoachClaims() {
    String token = jwtTokenProvider.generateCoachAccessToken(3L, 1);

    assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
    assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo("coach");
    assertThat(jwtTokenProvider.getCoachId(token)).isEqualTo(3L);
    assertThat(jwtTokenProvider.getCoachStatus(token)).isEqualTo(1);
  }

  @Test
  @DisplayName("过期 token 校验返回 false")
  void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
    JwtTokenProvider shortLivedProvider =
        new JwtTokenProvider(
            "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
            1L);
    String token = shortLivedProvider.generateUserAccessToken(1L, false);

    Thread.sleep(10);

    assertThat(shortLivedProvider.isTokenValid(token)).isFalse();
  }

  @Test
  @DisplayName("非法 token 校验返回 false")
  void isTokenValid_invalidToken_returnsFalse() {
    assertThat(jwtTokenProvider.isTokenValid("invalid.token.here")).isFalse();
  }

  @Test
  @DisplayName("refresh token 生成与哈希一致")
  void generateRefreshToken_hashRefreshToken_returnsConsistentHash() {
    String refreshToken = jwtTokenProvider.generateRefreshToken();
    String hash1 = jwtTokenProvider.hashRefreshToken(refreshToken);
    String hash2 = jwtTokenProvider.hashRefreshToken(refreshToken);

    assertThat(refreshToken).isNotBlank();
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(64);
  }

  @Test
  @DisplayName("不同 refresh token 哈希不同")
  void hashRefreshToken_differentTokens_returnsDifferentHashes() {
    String hash1 = jwtTokenProvider.hashRefreshToken(jwtTokenProvider.generateRefreshToken());
    String hash2 = jwtTokenProvider.hashRefreshToken(jwtTokenProvider.generateRefreshToken());

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  @DisplayName("getTokenType 返回 token 类型声明")
  void getTokenType_userToken_returnsUser() {
    String userToken = jwtTokenProvider.generateUserAccessToken(1L, false);

    assertThat(jwtTokenProvider.getTokenType(userToken)).isEqualTo("user");
  }
}
