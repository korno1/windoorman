package com.window.domain.windows.dto.response;

import com.window.domain.windows.entity.Windows;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WindowsResponseDto {

    private Long windowsId;
    private String name;
    private String state;
    private boolean isAuto;

    public static WindowsResponseDto createResponseDto(Windows windows, String state) {
        return new WindowsResponseDto(windows.getId(), windows.getName(), state, windows.isAuto());
    }
}
