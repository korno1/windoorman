package com.window.domain.monitoring.controllor;

import com.window.domain.monitoring.dto.GraphDataResponse;
import com.window.domain.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/sensors")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;
    @GetMapping(value = "/{windowId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getSensorData(@PathVariable("windowId") Long windowId) {
        return monitoringService.subscribe(windowId);

    }

    @GetMapping("/records/{windowId}/{category}/{range}")
    public ResponseEntity<?> getGraphByRange( @PathVariable("windowId") Long windowId, @PathVariable("category") int category, @PathVariable("range") int range) {

        List<GraphDataResponse> graphData = monitoringService.getGraphByRange(windowId, category, range);
        return ResponseEntity.ok(graphData);
    }



}
