package com.window.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRedisDto {

    private Long windowsId;
    private LocalTime endTime;
    private String deviceId;

}
