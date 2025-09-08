package com.window.global.listener; // 패키지 경로는 실제 프로젝트에 맞게 수정하세요.

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomSkipListener implements SkipListener<Object, Object> {

    private final ObjectMapper objectMapper;

    // Writer에서 아이템 처리 중 스킵이 발생했을 때 호출됩니다.
    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        try {
            // 어떤 아이템이 어떤 예외 때문에 스킵되었는지 JSON 형태로 로그를 남깁니다.
            // 이렇게 하면 나중에 로그를 파싱하여 실패한 데이터를 재처리하거나 원인을 분석하기 용이합니다.
            log.warn("SKIPPED_ITEM: item={}, exception={}",
                    objectMapper.writeValueAsString(item),
                    t.getMessage());
        } catch (Exception e) {
            log.error("Failed to log skipped item.", e);
        }
    }

    // Reader에서 스킵이 발생했을 때 호출됩니다. (현재 설정에서는 해당 없음)
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("SKIPPED_IN_READ: exception={}", t.getMessage());
    }

    // Processor에서 스킵이 발생했을 때 호출됩니다. (현재 설정에서는 해당 없음)
    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        try {
            log.warn("SKIPPED_IN_PROCESS: item={}, exception={}",
                    objectMapper.writeValueAsString(item),
                    t.getMessage());
        } catch (Exception e) {
            log.error("Failed to log skipped item in process.", e);
        }
    }
}