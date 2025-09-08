package com.window.domain.schedule.controller;

import com.window.domain.schedule.dto.request.ScheduleActivateRequestDto;
import com.window.domain.schedule.dto.request.ScheduleRequestDto;
import com.window.domain.schedule.dto.request.ScheduleUpdateRequestDto;
import com.window.domain.schedule.model.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<?> registerSchedule(@Valid @RequestBody ScheduleRequestDto dto, Authentication authentication) {
        Long id = scheduleService.registerSchedule(dto, authentication);

        return ResponseEntity.status(201).body(id);
    }

    @GetMapping
    public ResponseEntity<?> getSchedules(Authentication authentication) {
        return ResponseEntity.status(200).body(scheduleService.getSchedules(authentication));
    }

    @PatchMapping
    public ResponseEntity<?> updateSchedule(@Valid @RequestBody ScheduleUpdateRequestDto dto, Authentication authentication) {
        scheduleService.updateSchedule(dto, authentication);

        return ResponseEntity.status(200).body("스케줄 수정");

    }

    @PatchMapping("/toggle")
    public ResponseEntity<?> updateScheduleActivated(@Valid @RequestBody ScheduleActivateRequestDto dto) {
        scheduleService.updateScheduleActivate(dto);

        return ResponseEntity.status(200).body("활성화 수정");
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteSchedule(@PathVariable("groupId") Long groupId) {
        scheduleService.deleteSchedule(groupId);

        return ResponseEntity.status(200).body("스케줄 삭제");
    }
}
