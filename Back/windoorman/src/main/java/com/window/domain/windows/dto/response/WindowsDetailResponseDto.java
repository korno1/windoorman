package com.window.domain.windows.dto.response;

import com.window.domain.windows.dto.SensorDataDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WindowsDetailResponseDto {

    @Builder
    public WindowsDetailResponseDto(String placeName, Long windowsId, String name, SensorDataDto sensorData) {
        this.placeName = placeName;
        this.windowsId = windowsId;
        this.name = name;
        this.sensorData = sensorData;
    }

    private String placeName;
    private Long windowsId;
    private String name;
    SensorDataDto sensorData;

}
