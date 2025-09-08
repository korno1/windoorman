package com.window.domain.windows.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataDto {
    private Long windowsId;
    private Double co2;
    private Double voc;
    private Double pm25;
    private Double pm10;
    private Double humidity;
    private Double temperature;
}
