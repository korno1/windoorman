package com.window.domain.schedule.dto.request;

import com.window.domain.schedule.entity.Day;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class ScheduleUpdateRequestDto {

    @NotNull
    private Long groupId;

    @NotNull
    private Long windowsId;

    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;

    @NotEmpty
    List<Day> days;
}
