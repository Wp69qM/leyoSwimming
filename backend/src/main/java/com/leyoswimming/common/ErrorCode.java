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
  VALIDATION_ERROR(100005, "参数校验失败"),
  IDEMPOTENCY_DUPLICATE(100006, "请求正在处理中，请勿重复提交"),

  // 认证授权 200xxx
  UNAUTHORIZED(200001, "未登录或 Token 无效"),
  TOKEN_EXPIRED(200002, "登录已过期，请重新登录"),

  // 权限禁止 300xxx
  FORBIDDEN(300001, "无操作权限"),
  ADMIN_DISABLED(300002, "账号已被禁用，请联系超级管理员"),

  // 用户 400xxx
  USER_NOT_FOUND(400001, "用户不存在"),
  USER_DISABLED(400002, "用户账号已被封禁"),
  ACTIVE_PACKAGE_EXISTS(400201, "您还有未完成的套餐，无法注销"),
  PENDING_ORDER_EXISTS(400202, "您有未完成订单，请完成后注销"),
  ONGOING_BOOKING_EXISTS(400203, "您有未完成的课程预约，请完成后注销"),
  USER_ALREADY_DELETED(400204, "账号已注销"),

  // 教练 410xxx
  COACH_NOT_FOUND(410001, "教练不存在"),
  COACH_STATUS_NOT_ALLOWED(410002, "当前状态不可申请离职"),
  RESIGNATION_ALREADY_PENDING(410003, "已有进行中的离职申请"),
  TICKET_NOT_PROCESSING(410004, "工单状态不允许该操作"),
  NOT_OWN_PACKAGE(410005, "该套餐不属于当前教练"),

  // 管理员审批 510xxx
  CHECKLIST_NOT_PASSED(510001, "请先完成所有检查项"),
  SCHEDULE_NOT_CLEARED(510002, "未来排班未清空"),
  TICKET_NOT_PENDING_AUDIT(510003, "工单未提交至审批队列"),
  TICKET_ALREADY_PROCESSED(510004, "该工单已被处理"),

  // 验证码 420xxx
  INVALID_SMS_CODE(420001, "验证码错误或已过期"),
  SMS_RATE_LIMIT(420002, "请 60 秒后再试"),
  SMS_SEND_FAILED(420003, "验证码发送失败，请稍后重试"),

  // 微信 430xxx
  WECHAT_CODE_INVALID(430001, "登录凭证已失效，请重新点击登录"),
  WECHAT_API_ERROR(430002, "微信服务暂时不可用，请稍后重试"),
  WECHAT_API_TIMEOUT(430003, "网络异常，请重试"),
  PHONE_DECRYPT_FAILED(430004, "手机号解析失败"),

  // 协议 440xxx
  TERMS_NOT_ACCEPTED(440001, "请阅读并同意《用户须知》和《隐私协议》"),
  POLICY_NOT_FOUND(440002, "当前协议版本不存在或已下线"),
  CONSENT_VERSION_MISMATCH(440003, "协议版本已更新，请重新同意"),
  PROFILE_INCOMPLETE(440004, "请先完善个人资料"),
  NICKNAME_SENSITIVE(440005, "昵称包含敏感词，请修改后重试"),
  INVALID_FILE_TYPE(440006, "仅支持 JPG/PNG/WebP 图片格式"),
  FILE_TOO_LARGE(440007, "图片大小不能超过 5MB"),
  PHONE_ALREADY_BOUND(440008, "该手机号已被其他账号绑定"),
  INVALID_GENDER(440009, "性别参数错误"),
  INVALID_AGE(440010, "年龄需在 3-99 岁之间"),
  INVALID_SWIM_STROKE(440011, "泳姿参数错误"),

  // 教练入驻申请 500xxx
  COACH_APPLICATION_PENDING(500001, "已有待审核申请"),

  // 系统 900xxx
  INTERNAL_ERROR(900001, "系统繁忙，请稍后重试");

  private final int code;
  private final String message;
}
