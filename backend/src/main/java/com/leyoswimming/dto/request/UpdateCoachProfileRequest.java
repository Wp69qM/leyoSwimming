package com.leyoswimming.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCoachProfileRequest(
    @NotBlank @Size(max = 32) String name,
    @NotNull Integer age,
    @NotBlank @Pattern(regexp = "^(male|female)$") String gender,
    String avatarUrl,
    @Size(max = 512) String portraitUrl,
    @Size(max = 128) @Email String email,
    @Size(max = 512) String wechatQrUrl,
    @Size(max = 512) String personalDesc,
    Integer teachingYears,
    List<@Size(max = 128) String> certificates,
    List<@Pattern(regexp = "^(蛙泳|自由泳|仰泳|蝶泳)$") String> teachingStrokes,
    @Size(min = 10, max = 500) String bio,
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$") String newPhone,
    @Size(min = 6, max = 6) String oldPhoneVerifyCode,
    @Size(min = 6, max = 6) String newPhoneVerifyCode,
    @NotBlank @Size(max = 64) String idempotencyKey) {}
