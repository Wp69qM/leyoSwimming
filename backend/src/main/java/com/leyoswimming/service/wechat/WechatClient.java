package com.leyoswimming.service.wechat;

public interface WechatClient {

  WechatSession code2session(String code);

  String decryptPhone(String sessionKey, String encryptedData, String iv);
}
