package com.leyoswimming.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CoachProfileResponse(
    Long id,
    String name,
    String avatarUrl,
    String portraitUrl,
    String phone,
    Integer age,
    String gender,
    String email,
    String wechatQrUrl,
    String idCardNoMasked,
    String idCardFrontUrl,
    String idCardBackUrl,
    List<String> coachCertUrls,
    String healthCertUrl,
    Integer totalStudents,
    Integer totalHours,
    String personalDesc,
    Integer teachingYears,
    List<String> teachingStrokes,
    String bio,
    BigDecimal referencePrice,
    Integer status,
    List<String> certificates,
    Boolean profileCompleted,
    ConsentStatusResponse consent) {}
