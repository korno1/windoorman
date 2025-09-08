package com.window.domain.report.dto;

import lombok.Builder;

@Builder
public record AirReportDto(
        Long reportId,
        double lowTemperature,
        double highTemperature,
        double humidity,
        double airCondition
) {
}
