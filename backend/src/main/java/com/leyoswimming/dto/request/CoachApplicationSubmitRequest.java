package com.leyoswimming.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CoachApplicationSubmitRequest(
    @NotBlank(message = "姓名不能为空") @Size(max = 32, message = "姓名长度不能超过 32") String name,
    @NotBlank(message = "性别不能为空") String gender,
    @NotNull(message = "年龄不能为空") Integer age,
    @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
    @NotBlank(message = "微信二维码不能为空") String wechatQrUrl,
    @NotBlank(message = "身份证号不能为空") String idCardNo,
    @NotNull(message = "任教年限不能为空") Integer teachingYears,
    @NotNull(message = "总学员数不能为空") Integer totalStudents,
    @NotNull(message = "总课时数不能为空") Integer totalHours,
    @NotEmpty(message = "擅长泳姿不能为空") List<String> teachingStrokes,
    @NotBlank(message = "个人简介不能为空") String bio,
    @NotNull(message = "参考单价不能为空") BigDecimal referencePrice,
    @NotEmpty(message = "资质证书不能为空") @Valid List<CoachCertificateItem> certificates,
    String idempotencyKey) {}
