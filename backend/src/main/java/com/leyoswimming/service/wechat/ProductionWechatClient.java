package com.leyoswimming.service.wechat;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionWechatClient implements WechatClient {

  private static final String CODE2SESSION_URL =
      "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

  @Value("${leyo.wechat.appid}")
  private String appId;

  @Value("${leyo.wechat.secret}")
  private String secret;

  private final RestTemplate restTemplate;

  @Override
  public WechatSession code2session(String code) {
    try {
      ResponseEntity<Map> response =
          restTemplate.getForEntity(
              CODE2SESSION_URL, Map.class, appId, secret, code);
      Map<String, Object> body = response.getBody();
      if (body == null) {
        throw new BusinessException(ErrorCode.WECHAT_API_ERROR);
      }
      if (body.containsKey("errcode")) {
        log.error("Wechat code2session failed: {}", body);
        throw new BusinessException(ErrorCode.WECHAT_API_ERROR);
      }
      return new WechatSession(
          (String) body.get("openid"),
          (String) body.get("unionid"),
          (String) body.get("session_key"));
    } catch (RestClientException e) {
      log.error("Wechat code2session request failed", e);
      throw new BusinessException(ErrorCode.WECHAT_API_TIMEOUT);
    }
  }

  @Override
  public String decryptPhone(String sessionKey, String encryptedData, String iv) {
    try {
      byte[] sessionKeyBytes = Base64.getDecoder().decode(sessionKey);
      byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
      byte[] ivBytes = Base64.getDecoder().decode(iv);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(sessionKeyBytes, "AES"),
          new IvParameterSpec(ivBytes));
      String result = new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
      log.debug("Decrypted phone data: {}", result);
      return result;
    } catch (Exception e) {
      log.error("Failed to decrypt phone data", e);
      throw new BusinessException(ErrorCode.PHONE_DECRYPT_FAILED);
    }
  }
}
