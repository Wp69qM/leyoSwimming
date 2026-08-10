package com.leyoswimming.service.sms;

import com.leyoswimming.enums.AppType;

public interface SmsSender {

  void send(String phone, String code, String scene, AppType appType);
}
