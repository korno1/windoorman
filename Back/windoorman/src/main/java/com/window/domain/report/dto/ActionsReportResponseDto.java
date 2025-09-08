package com.window.domain.report.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ActionsReportResponseDto(
        Long actionReportId,
        String open,
        LocalDateTime openTime
) {
}
