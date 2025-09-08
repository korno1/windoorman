package com.window.domain.report.dto;

import lombok.Builder;

@Builder
public record WindowsDto(
        Long windowsId,
        String name
) {
}
