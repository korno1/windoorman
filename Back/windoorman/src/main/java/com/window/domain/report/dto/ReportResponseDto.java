package com.window.domain.report.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ReportResponseDto(
        AirReportDto airReport,
        List<WindowsDto> windows,
        List<ActionsReportResponseDto> actionsReport
) {
}
