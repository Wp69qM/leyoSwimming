package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountUpdateRequest(
    @NotNull(message = "管理员 ID 不能为空") Long adminId,
    @Size(min = 2, max = 32, message = "姓名需在 2-32 个字符之间") String name,
    @Pattern(regexp = "^(super_admin|admin)$", message = "角色只能是 super_admin 或 admin")
        String role,
    @NotNull(message = "版本号不能为空") Integer version) {}
