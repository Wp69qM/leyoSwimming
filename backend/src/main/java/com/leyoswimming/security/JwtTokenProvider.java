package com.leyoswimming.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
}
