package com.leyoswimming.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdCardEncryptor {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final String LEGACY_ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int NONCE_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final String VERSION_PREFIX = "v1:";
  private final SecretKeySpec keySpec;
  private final SecureRandom secureRandom;

  public IdCardEncryptor(@Value("${leyo.id-card-encryption.key}") String key) throws Exception {
    byte[] keyBytes =
        MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
    this.keySpec = new SecretKeySpec(keyBytes, "AES");
    this.secureRandom = new SecureRandom();
  }

  public String encrypt(String idCardNo) throws Exception {
    if (idCardNo == null) {
      return null;
    }
    byte[] nonce = new byte[NONCE_LENGTH];
    secureRandom.nextBytes(nonce);
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));
    byte[] ciphertext = cipher.doFinal(idCardNo.getBytes(StandardCharsets.UTF_8));
    String noncePart = Base64.getEncoder().encodeToString(nonce);
    String cipherPart = Base64.getEncoder().encodeToString(ciphertext);
    return VERSION_PREFIX + noncePart + ":" + cipherPart;
  }

  public String decrypt(String encrypted) throws Exception {
    if (encrypted == null || encrypted.isEmpty()) {
      return encrypted;
    }
    if (encrypted.startsWith(VERSION_PREFIX)) {
      String[] parts = encrypted.split(":", 3);
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid encrypted id card format");
      }
      byte[] nonce = Base64.getDecoder().decode(parts[1]);
      byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
    // Backward compatibility for data encrypted with the legacy CBC format.
    String[] parts = encrypted.split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid encrypted id card format");
    }
    byte[] iv = Base64.getDecoder().decode(parts[0]);
    byte[] ciphertext = Base64.getDecoder().decode(parts[1]);
    Cipher cipher = Cipher.getInstance(LEGACY_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
    return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
  }

  public static String mask(String idCardNo) {
    if (idCardNo == null || idCardNo.length() != 18) {
      return idCardNo;
    }
    return idCardNo.substring(0, 6) + "********" + idCardNo.substring(14);
  }
}
