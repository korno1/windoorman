package com.window.domain.schedule.model.service;

import com.window.domain.member.entity.Member;
import com.window.domain.schedule.dto.request.ScheduleActivateRequestDto;
import com.window.domain.schedule.dto.request.ScheduleRequestDto;
import com.window.domain.schedule.dto.request.ScheduleUpdateRequestDto;
import com.window.domain.schedule.dto.response.ScheduleResponseDto;
import com.window.domain.schedule.entity.Day;
import com.window.domain.schedule.entity.Schedule;
import com.window.domain.schedule.entity.ScheduleGroup;
import com.window.domain.schedule.model.repository.ScheduleGroupRepository;
import com.window.domain.schedule.model.repository.ScheduleRepository;
import com.window.domain.windows.entity.Windows;
import com.window.domain.windows.model.repository.WindowsRepository;
import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import com.window.global.util.MemberInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
// [수정] 클래스 레벨에 @Transactional(readOnly = true)를 선언하여, 조회 메서드의 성능을 최적화합니다.
// 데이터 변경이 있는 메서드에는 @Transactional을 별도로 붙여줍니다.
@Transactional(readOnly = true)
public class ScheduleService {

    private final WindowsRepository windowsRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleGroupRepository scheduleGroupRepository;

    @Transactional
    public Long registerSchedule(ScheduleRequestDto dto, Authentication authentication) {
        ScheduleGroup scheduleGroup = scheduleGroupRepository.save(new ScheduleGroup(LocalDateTime.now()));
        Member member = MemberInfo.getMemberInfo(authentication);
        Windows windows = windowsRepository.findById(dto.getWindowsId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        // [수정] 여러 개의 Schedule 엔티티를 한번에 저장하기 위해 saveAll 사용
        List<Schedule> schedules = new ArrayList<>();
        for (Day day : dto.getDays()) {
            schedules.add(Schedule.builder()
                    .scheduleGroup(scheduleGroup)
                    .windows(windows)
                    .member(member)
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .day(day)
                    .build());
        }
        scheduleRepository.saveAll(schedules);

        return scheduleGroup.getId();
    }

    public List<ScheduleResponseDto> getSchedules(Authentication authentication) {
        Member member = MemberInfo.getMemberInfo(authentication);
        // [수정] N+1 문제를 해결한 레포지토리 메서드(findAllByMemberIdWithDetails)를 사용하여 성능 개선
        // [수정] Optional을 반환하지 않으므로 orElseThrow 제거
        List<Schedule> schedules = scheduleRepository.findAllByMemberIdWithDetails(member.getId());

        Map<Long, ScheduleResponseDto> scheduleMap = new HashMap<>();
        for (Schedule schedule : schedules) {
            // [수정] computeIfAbsent를 사용하여 코드를 더 간결하게 만듦
            scheduleMap.computeIfAbsent(schedule.getScheduleGroup().getId(), k -> ScheduleResponseDto.builder()
                    .scheduleId(schedule.getId())
                    .groupId(schedule.getScheduleGroup().getId())
                    .windowsId(schedule.getWindows().getId())
                    .placeName(schedule.getWindows().getPlace().getName())
                    .windowName(schedule.getWindows().getName())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .days(new ArrayList<>())
                    .isActivate(schedule.getScheduleGroup().isActivate())
                    .build());

            scheduleMap.get(schedule.getScheduleGroup().getId()).getDays().add(schedule.getDay());
        }

        return new ArrayList<>(scheduleMap.values());
    }

    @Transactional
    public void updateSchedule(ScheduleUpdateRequestDto dto, Authentication authentication) {
        ScheduleGroup scheduleGroup = scheduleGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_SCHEDULEGROUP_EXCEPTION));

        // [수정] 기존 ScheduleGroup을 삭제하고 새로 만드는 대신, 기존 그룹을 유지하고 연관된 Schedule만 갱신
        // 이를 통해 groupId가 변경되지 않아 클라이언트와의 데이터 일관성을 유지합니다.
        scheduleRepository.deleteAllByScheduleGroup(scheduleGroup);

        Windows windows = windowsRepository.findById(dto.getWindowsId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));
        Member member = MemberInfo.getMemberInfo(authentication);

        List<Schedule> newSchedules = new ArrayList<>();
        for (Day day : dto.getDays()) {
            newSchedules.add(Schedule.builder()
                    .scheduleGroup(scheduleGroup) // 기존 그룹 ID 유지
                    .windows(windows)
                    .member(member)
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .day(day)
                    .build());
        }
        scheduleRepository.saveAll(newSchedules);
    }

    @Transactional
    public void updateScheduleActivate(ScheduleActivateRequestDto dto) {
        ScheduleGroup scheduleGroup = scheduleGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_SCHEDULEGROUP_EXCEPTION));
        scheduleGroup.updateActive(dto);
        // [수정] @Transactional에 의해 엔티티가 관리되므로(더티 체킹), 명시적인 save 호출이 필요 없어 제거.
    }

    @Transactional
    public void deleteSchedule(Long groupId) {
        // [수정] 삭제 전 존재 여부를 먼저 확인하여 명확한 예외를 발생시킴
        if (!scheduleGroupRepository.existsById(groupId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_SCHEDULEGROUP_EXCEPTION);
        }
        // [수정] 연관된 Schedule을 먼저 삭제하고 그룹을 삭제하여 데이터 무결성 보장
        scheduleRepository.deleteAllByScheduleGroupId(groupId);
        scheduleGroupRepository.deleteById(groupId);
    }
}