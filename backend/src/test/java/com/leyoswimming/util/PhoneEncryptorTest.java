package com.leyoswimming.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhoneEncryptorTest {

  private PhoneEncryptor phoneEncryptor;

  @BeforeEach
  void setUp() throws Exception {
    phoneEncryptor = new PhoneEncryptor("local-test-phone-encryption-key-32bytes!");
  }

  @Test
  @DisplayName("加密解密手机号保持一致")
  void encryptDecrypt_roundtrip_returnsOriginal() throws Exception {
    String phone = "13800138000";

    String encrypted = phoneEncryptor.encrypt(phone);
    String decrypted = phoneEncryptor.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(phone);
  }

  @Test
  @DisplayName("相同手机号使用相同密钥加密结果一致")
  void encrypt_samePhone_sameKey_returnsSameCipher() throws Exception {
    String phone = "13800138000";

    String encrypted1 = phoneEncryptor.encrypt(phone);
    String encrypted2 = phoneEncryptor.encrypt(phone);

    assertThat(encrypted1).isEqualTo(encrypted2);
  }

  @Test
  @DisplayName("不同手机号加密结果不同")
  void encrypt_differentPhones_returnsDifferentCipher() throws Exception {
    String encrypted1 = phoneEncryptor.encrypt("13800138000");
    String encrypted2 = phoneEncryptor.encrypt("13800138001");

    assertThat(encrypted1).isNotEqualTo(encrypted2);
  }

  @Test
  @DisplayName("不同密钥解密失败")
  void decrypt_differentKey_throwsException() throws Exception {
    String encrypted = phoneEncryptor.encrypt("13800138000");
    PhoneEncryptor otherEncryptor =
        new PhoneEncryptor("different-key-for-phone-encryption-32bytes");

    assertThatThrownBy(() -> otherEncryptor.decrypt(encrypted)).isInstanceOf(Exception.class);
  }
}
