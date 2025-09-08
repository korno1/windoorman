package com.window.domain.windowAction.dto.response;

import lombok.Builder;

@Builder
public record AvgActionResponseDto(
        double avgActions
) {
}
