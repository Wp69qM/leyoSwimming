package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUserAddRequest(
    String avatarUrl,
    @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
    @NotBlank(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过 64 个字符")
        String name,
    @NotNull(message = "性别不能为空") Integer gender,
    @NotNull(message = "年龄不能为空") Integer age,
    Boolean hasSwimBasis,
    List<String> swimStrokes,
    Integer swimYears,
    @Size(max = 512, message = "个人描述不能超过 512 个字符") String personalDesc,
    String guardianName,
    String guardianPhone) {}
