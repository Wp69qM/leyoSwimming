package com.leyoswimming.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhoneEncryptor {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private final SecretKeySpec keySpec;
  private final IvParameterSpec ivSpec;

  public PhoneEncryptor(@Value("${leyo.phone-encryption.key}") String key) throws Exception {
    byte[] keyBytes =
        MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
    this.keySpec = new SecretKeySpec(keyBytes, "AES");
    this.ivSpec = new IvParameterSpec(new byte[16]);
  }

  public String encrypt(String phone) throws Exception {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    return Base64.getEncoder()
        .encodeToString(cipher.doFinal(phone.getBytes(StandardCharsets.UTF_8)));
  }

  public String decrypt(String encrypted) throws Exception {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    return new String(
        cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);
  }
}
