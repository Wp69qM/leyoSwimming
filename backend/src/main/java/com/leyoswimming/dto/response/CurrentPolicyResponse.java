package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record CurrentPolicyResponse(String version, String content, LocalDateTime effectiveAt) {}
