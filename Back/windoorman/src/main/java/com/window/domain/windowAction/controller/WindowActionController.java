package com.window.domain.windowAction.controller;

import com.window.domain.windowAction.dto.response.AvgActionResponseDto;
import com.window.domain.windowAction.dto.request.WindowActionRequestDto;
import com.window.domain.windowAction.service.WindowActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/actions")
public class WindowActionController {

    private final WindowActionService windowActionService;

    @PostMapping
    public ResponseEntity<?> registerWindowAction(@RequestBody WindowActionRequestDto windowAction) {
        Long id = windowActionService.registerWindowAction(windowAction);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/avg/{placeId}")
    public ResponseEntity<?> findAvgAction(@PathVariable Long placeId) {
        AvgActionResponseDto response = windowActionService.findCountAction(placeId);

        return ResponseEntity.ok(response);
    }
}
