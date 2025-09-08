package com.window.domain.windows.controller;

import com.window.domain.windows.dto.request.WindowsRequestDto;
import com.window.domain.windows.dto.request.WindowsToggleRequestDto;
import com.window.domain.windows.dto.request.WindowsUpdateRequestDto;
import com.window.domain.windows.dto.response.WindowsDetailResponseDto;
import com.window.domain.windows.model.service.WindowsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/windows")
@RequiredArgsConstructor
@Validated
@Slf4j
public class WindowsController {

    private final WindowsService windowService;

    // [수정] 불필요한 ObjectMapper 의존성 제거
    // private final ObjectMapper objectMapper;

    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Object>> getWindows(@PathVariable Long placeId) {
        Map<String, Object> windows = windowService.getWindows(placeId);
        return ResponseEntity.ok(windows);
    }

    @GetMapping("/detail/{windowsId}")
    public ResponseEntity<WindowsDetailResponseDto> getWindow(@PathVariable Long windowsId) {
        WindowsDetailResponseDto dto = windowService.getWindowInfo(windowsId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    // [수정] registerWindow 메서드에서 사용하지 않는 Authentication 파라미터 제거
    public ResponseEntity<Long> registerWindow(@Valid @RequestBody WindowsRequestDto dto) {
        log.info("창문 등록 정보 {}", dto.getName());
        Long windowsId = windowService.registerWindow(dto);
        return ResponseEntity.status(201).body(windowsId);
    }

    @PatchMapping
    public ResponseEntity<String> updateWindow(@RequestBody WindowsUpdateRequestDto dto) {
        windowService.updateWindow(dto);
        return ResponseEntity.ok("수정");
    }

    @DeleteMapping("/{windowsId}")
    public ResponseEntity<String> deleteWindow(@PathVariable Long windowsId) {
        windowService.deleteWindow(windowsId);
        return ResponseEntity.ok("삭제");
    }

    @PatchMapping("/toggle")
    public ResponseEntity<String> toggleChange(@RequestBody WindowsToggleRequestDto dto) {
        windowService.changeToggle(dto);
        log.info("{}", dto.getIsAuto());
        return ResponseEntity.ok("활성화 변경");
    }

    // [수정] 컨트롤러에서 JSON 파싱 로직 제거
    // 서비스 계층에서 받은 응답(JSON 문자열)을 그대로 반환하도록 변경
    @GetMapping("/open/{windowsId}")
    public ResponseEntity<String> openWindow(@PathVariable("windowsId") Long windowsId) {
        String data = windowService.open(windowsId, false);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/close/{windowsId}")
    public ResponseEntity<String> closeWindow(@PathVariable("windowsId") Long windowsId) {
        String data = windowService.close(windowsId, false);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/open/auto/{windowsId}")
    public ResponseEntity<String> openAutoWindow(@PathVariable("windowsId") Long windowsId) {
        String data = windowService.open(windowsId, true);
        if (data == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/close/auto/{windowsId}")
    public ResponseEntity<String> closeAutoWindow(@PathVariable("windowsId") Long windowsId) {
        String data = windowService.close(windowsId, true);
        if (data == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/state/{windowsId}")
    public ResponseEntity<String> getState(@PathVariable("windowsId") Long windowsId) {
        return ResponseEntity.ok(windowService.getState(windowsId));
    }
}