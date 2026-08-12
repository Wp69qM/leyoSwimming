package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminCoachUpdateRequest(
    @NotNull(message = "教练 ID 不能为空") Long coachId,
    @NotNull(message = "版本号不能为空") Integer version,
    String avatarUrl,
    @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
    @NotBlank(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过 64 个字符")
        String name,
    @NotBlank(message = "性别不能为空") String gender,
    @NotNull(message = "年龄不能为空") Integer age,
    @Size(max = 128, message = "邮箱长度不能超过 128 个字符") String email,
    String wechatQrUrl,
    @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "\\d{17}[\\dXx]", message = "请输入 18 位有效身份证号")
        String idCardNo,
    @NotNull(message = "任教年限不能为空") Integer teachingYears,
    @NotNull(message = "总学员数不能为空") Integer totalStudents,
    @NotNull(message = "总课时数不能为空") Integer totalHours,
    List<String> teachingStrokes,
    @Size(max = 2000, message = "个人简介不能超过 2000 个字符") String bio,
    @NotNull(message = "参考单价不能为空") BigDecimal referencePrice,
    @NotNull(message = "资质证书不能为空") List<CoachCertificateItem> certificates) {}
