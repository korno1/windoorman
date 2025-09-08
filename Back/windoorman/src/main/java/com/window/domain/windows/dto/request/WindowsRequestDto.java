package com.window.domain.windows.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class WindowsRequestDto {

    @NotNull
    private Long placeId;

    @NotEmpty
    private String name;

    @NotEmpty
    private String deviceId;

}
