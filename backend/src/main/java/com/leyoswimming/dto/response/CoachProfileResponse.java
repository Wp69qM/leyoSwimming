package com.leyoswimming.dto.response;

import java.util.List;

public record CoachProfileResponse(
    Long id,
    String name,
    String avatarUrl,
    String phone,
    Integer age,
    String gender,
    String personalDesc,
    Integer teachingYears,
    List<String> certificates,
    Boolean profileCompleted,
    ConsentStatusResponse consent) {}
