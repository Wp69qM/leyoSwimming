package com.leyoswimming.dto.response;

import java.util.List;

public record UserProfileResponse(
    Long id,
    String name,
    String avatarUrl,
    String phone,
    Integer age,
    String gender,
    String guardianName,
    String guardianPhone,
    Boolean hasSwimBasis,
    List<String> swimStrokes,
    Integer swimYears,
    String personalDesc,
    Boolean profileCompleted,
    ConsentStatusResponse consent) {}
