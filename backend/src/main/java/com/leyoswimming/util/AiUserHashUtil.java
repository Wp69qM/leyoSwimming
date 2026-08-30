package com.leyoswimming.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AiUserHashUtil {

  private static final String HASH_PREFIX = "u_";
  private static final String HASH_SEED = "leyo-ai:";

  private AiUserHashUtil() {}

  public static String hash(Long userId) {
    if (userId == null) {
      return null;
    }
    return HASH_PREFIX + sha256Hex(HASH_SEED + userId).substring(0, 16);
  }

  public static String coachHash(Long coachId) {
    if (coachId == null) {
      return null;
    }
    return "c_" + sha256Hex("leyo-coach:" + coachId).substring(0, 16);
  }

  public static String packageHash(Long packageId) {
    if (packageId == null) {
      return null;
    }
    return "p_" + sha256Hex("leyo-package:" + packageId).substring(0, 16);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
