package com.leyoswimming.controller.common;

import com.leyoswimming.common.ApiResponse;
import com.leyoswimming.dto.request.SendSmsRequest;
import com.leyoswimming.enums.AppType;
import com.leyoswimming.service.SmsCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/common/sms")
@RequiredArgsConstructor
public class SmsController {

  private final SmsCodeService smsCodeService;

  @PostMapping("/send")
  public ApiResponse<Void> send(@Valid @RequestBody SendSmsRequest request) {
    smsCodeService.send(request.phone(), request.scene(), AppType.valueOf(request.appType()));
    return ApiResponse.ok(null);
  }
}
