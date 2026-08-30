package com.leyoswimming.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey accessTokenKey;
  private final long accessTokenExpirationSeconds;

  public JwtTokenProvider(
      @Value("${leyo.jwt.secret}") String secret,
      @Value("${leyo.jwt.access-token-expiration:86400000}") long accessTokenExpirationMs) {
    this.accessTokenKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.accessTokenExpirationSeconds = accessTokenExpirationMs / 1000;
  }

  public String generateAdminToken(Long adminUserId, String username) {
    Instant now = Instant.now();
    Instant expiry = now.plus(accessTokenExpirationSeconds, ChronoUnit.SECONDS);
    return Jwts.builder()
        .subject(String.valueOf(adminUserId))
        .claim("username", username)
        .claim("type", "admin")
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(accessTokenKey)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(accessTokenKey).build().parseSignedClaims(token).getPayload();
  }

  public Long getAdminUserId(String token) {
    return Long.valueOf(parseToken(token).getSubject());
  }

  public boolean isTokenValid(String token) {
    try {
      parseToken(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.debug("Token expired");
      return false;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Invalid token: {}", e.getMessage());
      return false;
    }
  }

  public long getExpirationSeconds() {
    return accessTokenExpirationSeconds;
  }

  public String generateUserAccessToken(Long userId, boolean profileCompleted) {
    return buildToken(userId.toString(), Map.of("type", "user", "profileCompleted", profileCompleted));
  }

  public String generateCoachAccessToken(Long coachId, int coachStatus) {
    return buildToken(coachId.toString(), Map.of("type", "coach", "coachStatus", coachStatus));
  }

  private String buildToken(String subject, Map<String, Object> claims) {
    Instant now = Instant.now();
    Instant expiry = now.plus(accessTokenExpirationSeconds, ChronoUnit.SECONDS);
    JwtBuilder builder =
        Jwts.builder().subject(subject).issuedAt(Date.from(now)).expiration(Date.from(expiry));
    claims.forEach(builder::claim);
    return builder.signWith(accessTokenKey).compact();
  }

  public String generateRefreshToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String hashRefreshToken(String refreshToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  public String getTokenType(String token) {
    return parseToken(token).get("type", String.class);
  }

  public Long getUserId(String token) {
    return Long.valueOf(parseToken(token).getSubject());
  }

  public Long getCoachId(String token) {
    return Long.valueOf(parseToken(token).getSubject());
  }

  public boolean isProfileCompleted(String token) {
    return Boolean.TRUE.equals(parseToken(token).get("profileCompleted", Boolean.class));
  }

  public int getCoachStatus(String token) {
    return parseToken(token).get("coachStatus", Integer.class);
  }
}
