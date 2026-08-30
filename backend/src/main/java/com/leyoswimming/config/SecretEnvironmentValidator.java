package com.leyoswimming.config;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
public class SecretEnvironmentValidator {

  private static final Set<String> WEAK_SECRETS =
      Set.of(
          "bG9jYWwtZGV2LXNlY3JldC1tdXN0LWJlLW92ZXJyaWRkZW4taW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0zMi1ieXRlcw==",
          "local-dev-phone-encryption-key-32bytes!",
          "local-test-phone-encryption-key-32bytes!",
          "change-me",
          "secret",
          "jwt-secret",
          "phone-secret");

  @Value("${leyo.jwt.secret:}")
  private String jwtSecret;

  @Value("${leyo.phone-encryption.key:}")
  private String phoneEncryptionKey;

  @PostConstruct
  public void validate() {
    validateNotEmpty("JWT_SECRET", jwtSecret);
    validateNotEmpty("PHONE_ENCRYPTION_KEY", phoneEncryptionKey);
    validateMinimumLength("JWT_SECRET", jwtSecret, 32);
    validateMinimumLength("PHONE_ENCRYPTION_KEY", phoneEncryptionKey, 32);
    validateNotWeak("JWT_SECRET", jwtSecret);
    validateNotWeak("PHONE_ENCRYPTION_KEY", phoneEncryptionKey);
  }

  private void validateNotEmpty(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          name
              + " must be set. For local development, configure it via environment variable or IDE run configuration.");
    }
  }

  private void validateMinimumLength(String name, String value, int minBytes) {
    byte[] bytes = value.getBytes();
    if (bytes.length < minBytes) {
      throw new IllegalStateException(
          name + " must be at least " + minBytes + " bytes, got " + bytes.length);
    }
  }

  private void validateNotWeak(String name, String value) {
    if (WEAK_SECRETS.contains(value)) {
      throw new IllegalStateException(name + " is using a known weak/default value: " + value);
    }
    try {
      String decoded = new String(Base64.getDecoder().decode(value));
      if (WEAK_SECRETS.contains(decoded)) {
        throw new IllegalStateException(
            name + " is a Base64 encoding of a known weak/default value");
      }
    } catch (IllegalArgumentException ignored) {
      // not Base64, fine
    }
  }
}
