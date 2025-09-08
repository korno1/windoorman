package com.window.domain.monitoring.repository;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
@ToString
public class EmitterRepository {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(long windowId, SseEmitter emitter) {
        emitters.put(windowId, emitter);
    }
    public void deleteById(long windowId) {
        emitters.remove(windowId);
    }
    public SseEmitter get(long windowId) {
        return emitters.get(windowId);
    }
    public int size() {
        return emitters.size();
    }

    public Set<Long> getKeySet() {
        return emitters.keySet();
    }
}
