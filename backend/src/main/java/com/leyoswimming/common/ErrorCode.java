package com.leyoswimming.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  // 通用 100xxx
  BAD_REQUEST(100001, "请求参数错误"),
  JSON_PARSE_ERROR(100002, "请求体 JSON 解析失败"),
  MISSING_REQUIRED_PARAM(100003, "必填参数缺失"),
  RESOURCE_NOT_FOUND(100004, "资源不存在"),

  // 认证授权 200xxx
  UNAUTHORIZED(200001, "未登录或 Token 无效"),
  TOKEN_EXPIRED(200002, "登录已过期，请重新登录"),

  // 权限禁止 300xxx
  FORBIDDEN(300001, "无操作权限"),
  ADMIN_DISABLED(300002, "账号已被禁用，请联系超级管理员"),

  // 教练入驻申请 500xxx
  COACH_APPLICATION_PENDING(500001, "已有待审核申请"),

  // 系统 900xxx
  INTERNAL_ERROR(900001, "系统繁忙，请稍后重试");

  private final int code;
  private final String message;
}
