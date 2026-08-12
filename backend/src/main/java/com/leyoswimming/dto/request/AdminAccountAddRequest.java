package com.leyoswimming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountAddRequest(
    @NotBlank(message = "姓名不能为空")
        @Size(min = 2, max = 32, message = "姓名需在 2-32 个字符之间")
        String name,
    @NotBlank(message = "登录账号不能为空")
        @Size(min = 3, max = 32, message = "登录账号需在 3-32 个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "登录账号只能包含字母、数字、下划线")
        String username,
    @NotBlank(message = "初始密码不能为空")
        @Size(min = 8, max = 32, message = "密码需在 8-32 个字符之间")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&_#^])[A-Za-z\\d@$!%*?&_#^]+$",
            message = "密码需同时包含字母、数字和特殊字符")
        String password,
    @NotBlank(message = "角色不能为空") @Pattern(regexp = "^(super_admin|admin)$", message = "角色只能是 super_admin 或 admin")
        String role) {}
