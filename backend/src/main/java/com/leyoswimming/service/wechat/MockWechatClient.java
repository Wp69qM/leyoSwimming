package com.leyoswimming.service.wechat;

import com.leyoswimming.common.ErrorCode;
import com.leyoswimming.exception.BusinessException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "leyo.wechat", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockWechatClient implements WechatClient {

  @Override
  public WechatSession code2session(String code) {
    if ("expired".equals(code)) {
      throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
    }
    String seed = UUID.nameUUIDFromBytes(code.getBytes()).toString().replace("-", "");
    return new WechatSession(
        "mock_openid_" + seed.substring(0, 8),
        "mock_union_" + seed.substring(8, 16),
        "mock_session_key_" + seed);
  }

  @Override
  public String decryptPhone(String sessionKey, String encryptedData, String iv) {
    return "13800138000";
  }
}
