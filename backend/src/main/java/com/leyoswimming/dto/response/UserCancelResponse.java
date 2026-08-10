package com.leyoswimming.dto.response;

import java.time.LocalDateTime;

public record UserCancelResponse(boolean cancelled, LocalDateTime anonymousAfter) {}
