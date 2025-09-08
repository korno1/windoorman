package com.window.domain.windows.controller;

import com.window.domain.windows.model.service.WindowsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeviceController {

    private final WindowsService windowsService;

    @GetMapping("/devices")
    public ResponseEntity<?> getDevices(){
        return ResponseEntity.ok(windowsService.getDevices());
    }
}
