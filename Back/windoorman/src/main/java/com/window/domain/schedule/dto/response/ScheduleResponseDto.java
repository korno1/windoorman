package com.window.domain.schedule.dto.response;

import com.window.domain.schedule.entity.Day;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class ScheduleResponseDto {

    @Builder
    public ScheduleResponseDto(Long scheduleId, Long groupId, Long windowsId, String placeName, String windowName, LocalTime startTime, LocalTime endTime, List<Day> days, boolean isActivate) {
        this.scheduleId = scheduleId;
        this.groupId = groupId;
        this.windowsId = windowsId;
        this.placeName = placeName;
        this.windowName = windowName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.days = days;
        this.isActivate = isActivate;
    }

    private Long scheduleId;
    private Long groupId;
    private Long windowsId;
    private String placeName;
    private String windowName;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Day> days;
    private boolean isActivate;

}
