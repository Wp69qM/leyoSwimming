package com.leyoswimming.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiUserHashUtilTest {

  @Test
  @DisplayName("hash: 相同 userId 输出稳定且格式正确")
  void hash_sameUserId_returnsStableHash() {
    String first = AiUserHashUtil.hash(12345L);
    String second = AiUserHashUtil.hash(12345L);

    assertThat(first).isEqualTo(second);
    assertThat(first).startsWith("u_");
    assertThat(first).hasSize(18);
  }

  @Test
  @DisplayName("hash: null userId 返回 null")
  void hash_nullUserId_returnsNull() {
    assertThat(AiUserHashUtil.hash(null)).isNull();
  }

  @Test
  @DisplayName("coachHash: 相同 coachId 输出稳定且格式正确")
  void coachHash_sameCoachId_returnsStableHash() {
    String first = AiUserHashUtil.coachHash(67890L);
    String second = AiUserHashUtil.coachHash(67890L);

    assertThat(first).isEqualTo(second);
    assertThat(first).startsWith("c_");
    assertThat(first).hasSize(18);
  }

  @Test
  @DisplayName("coachHash: null coachId 返回 null")
  void coachHash_nullCoachId_returnsNull() {
    assertThat(AiUserHashUtil.coachHash(null)).isNull();
  }

  @Test
  @DisplayName("packageHash: 相同 packageId 输出稳定且格式正确")
  void packageHash_samePackageId_returnsStableHash() {
    String first = AiUserHashUtil.packageHash(11111L);
    String second = AiUserHashUtil.packageHash(11111L);

    assertThat(first).isEqualTo(second);
    assertThat(first).startsWith("p_");
    assertThat(first).hasSize(18);
  }

  @Test
  @DisplayName("packageHash: null packageId 返回 null")
  void packageHash_nullPackageId_returnsNull() {
    assertThat(AiUserHashUtil.packageHash(null)).isNull();
  }

  @Test
  @DisplayName("不同 ID 的 hash 值不同")
  void hash_differentIds_returnsDifferentHashes() {
    String userHash = AiUserHashUtil.hash(1L);
    String coachHash = AiUserHashUtil.coachHash(1L);
    String packageHash = AiUserHashUtil.packageHash(1L);

    assertThat(userHash).isNotEqualTo(coachHash);
    assertThat(userHash).isNotEqualTo(packageHash);
    assertThat(coachHash).isNotEqualTo(packageHash);
  }
}
