package com.window.domain.windowAction.service;

import com.window.domain.windowAction.dto.response.AvgActionResponseDto;
import com.window.domain.windowAction.dto.request.WindowActionRequestDto;
import com.window.domain.windowAction.entity.WindowAction;
import com.window.domain.windowAction.repository.WindowActionRepository;
import com.window.domain.windows.entity.Windows;
import com.window.domain.windows.model.repository.WindowsRepository;
import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WindowActionService {

    private final WindowActionRepository windowActionRepository;
    private final WindowsRepository windowsRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.redis.set.key}")
    private String redisSetKey;

    @Value("${spring.redis.action.key}")
    private String actionKey;

    @Transactional
    public Long registerWindowAction(WindowActionRequestDto dto) {
        // [수정] 복잡한 if-else 블록을 별도의 private 메서드로 분리하여 가독성 향상
        if (isDuplicateScheduledAction(dto.getWindowsId())) {
            return null;
        }

        Windows windows = windowsRepository.findById(dto.getWindowsId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        if (!windows.isAuto()) {
            return null;
        }

        // [수정] DTO -> Entity 변환 로직을 WindowAction 엔티티의 정적 팩토리 메서드로 위임
        WindowAction windowAction = WindowAction.of(dto, windows);
        return windowActionRepository.save(windowAction).getId();
    }

    /**
     * [추가] 스케줄에 의한 자동 제어 액션이 중복 기록되는 것을 방지하는 로직
     * @return 중복이라면 true, 아니라면 false
     */
    private boolean isDuplicateScheduledAction(Long windowsId) {
        String windowsIdStr = String.valueOf(windowsId);
        boolean isScheduled = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisSetKey, windowsIdStr));
        boolean alreadyActioned = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(actionKey, windowsIdStr));

        if (isScheduled) {
            if (alreadyActioned) {
                return true; // 스케줄 실행 중, 이미 액션 기록됨 -> 중복이므로 중단
            }
            // 스케줄 실행 중, 첫 액션 기록
            redisTemplate.opsForSet().add(actionKey, windowsIdStr);
        } else {
            // 스케줄 실행 중이 아닐 때, 혹시 남아있을 수 있는 액션 플래그 제거
            if (alreadyActioned) {
                redisTemplate.opsForSet().remove(actionKey, windowsIdStr);
            }
        }
        return false; // 중복 아님, 계속 진행
    }

    public AvgActionResponseDto findCountAction(Long placeId) {
        LocalDateTime endOfDay = LocalDateTime.now();
        LocalDateTime startOfDay = endOfDay.minusDays(7);

        Long openCount = windowActionRepository.countByCountAction(placeId, startOfDay, endOfDay);
        Long windowsCount = windowsRepository.countByPlace_Id(placeId);

        log.info("countActions : openCount {} windowsCount {}", openCount, windowsCount);

        // [수정] 0으로 나누기 예외(ArithmeticException) 방지를 위한 방어 코드 추가
        if (windowsCount == 0) {
            return new AvgActionResponseDto(0.0);
        }

        return new AvgActionResponseDto((double) openCount / windowsCount);
    }
}