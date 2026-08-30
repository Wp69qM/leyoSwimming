package com.leyoswimming.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CoachApplicationSaveDraftRequest(
    Long applicationId,
    @Size(max = 32, message = "姓名长度不能超过 32") String name,
    String gender,
    Integer age,
    @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱长度不能超过 128") String email,
    String wechatQrUrl,
    String idCardNo,
    Integer teachingYears,
    Integer totalStudents,
    Integer totalHours,
    List<String> teachingStrokes,
    @Size(min = 10, max = 500, message = "个人简介需在 10-500 字符之间") String bio,
    BigDecimal referencePrice,
    @Valid List<CoachCertificateItem> certificates,
    String idempotencyKey) {}
