package com.window.domain.schedule.dto.request;

import com.window.domain.schedule.entity.Day;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@Getter
public class ScheduleRequestDto {
    @NotNull
    private Long windowsId;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotEmpty
    List<Day> days;
}
