package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUserUpdateRequest(
    @NotNull(message = "用户 ID 不能为空") Long userId,
    String avatarUrl,
    @NotNull(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过 64 个字符")
        String name,
    @NotNull(message = "性别不能为空") Integer gender,
    @NotNull(message = "年龄不能为空") @Min(value = 3, message = "年龄不能小于 3 岁") Integer age,
    @NotNull(message = "是否有游泳基础不能为空") Boolean hasSwimBasis,
    List<String> swimStrokes,
    Integer swimYears,
    @Size(max = 512, message = "个人描述不能超过 512 个字符") String personalDesc,
    String guardianName,
    String guardianPhone,
    @NotNull(message = "版本号不能为空") Integer version) {}
