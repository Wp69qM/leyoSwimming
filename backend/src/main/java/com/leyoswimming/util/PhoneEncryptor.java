package com.leyoswimming.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhoneEncryptor {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int IV_LENGTH = 16;
  private final SecretKeySpec keySpec;
  private final SecureRandom secureRandom;

  public PhoneEncryptor(@Value("${leyo.phone-encryption.key}") String key) throws Exception {
    byte[] keyBytes =
        MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
    this.keySpec = new SecretKeySpec(keyBytes, "AES");
    this.secureRandom = SecureRandom.getInstanceStrong();
  }

  public String encrypt(String phone) throws Exception {
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
    byte[] ciphertext = cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8));
    String ivPart = Base64.getEncoder().encodeToString(iv);
    String cipherPart = Base64.getEncoder().encodeToString(ciphertext);
    return ivPart + ":" + cipherPart;
  }

  public String decrypt(String encrypted) throws Exception {
    if (encrypted == null || encrypted.isEmpty()) {
      return encrypted;
    }
    if (encrypted.contains(":")) {
      String[] parts = encrypted.split(":", 2);
      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
    // Backward compatibility for data encrypted with the legacy fixed IV.
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(new byte[IV_LENGTH]));
    return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
  }

  public String hash(String phone) throws Exception {
    if (phone == null) {
      return null;
    }
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(keySpec.getEncoded(), "HmacSHA256"));
    return Base64.getEncoder().encodeToString(mac.doFinal(phone.getBytes(StandardCharsets.UTF_8)));
  }

  public static String mask(String phone) {
    if (phone == null || phone.length() != 11) {
      return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(7);
  }

  String encryptWithFixedIvForTest(String phone) throws Exception {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(new byte[IV_LENGTH]));
    return Base64.getEncoder().encodeToString(cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8)));
  }
}
