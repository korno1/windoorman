package com.window.domain.windowAction.dto.request;

import com.window.domain.windowAction.dto.ReasonDto;
import com.window.domain.windowAction.entity.Open;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class WindowActionRequestDto {
    private Long windowsId;
    private Open open;
    private LocalDateTime openTime;
    private List<ReasonDto> reason;
}
