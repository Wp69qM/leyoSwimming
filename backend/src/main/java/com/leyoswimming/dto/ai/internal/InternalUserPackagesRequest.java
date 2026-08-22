package com.leyoswimming.dto.ai.internal;

import java.util.List;

public record InternalUserPackagesRequest(Long userId, List<String> statuses) {
}
