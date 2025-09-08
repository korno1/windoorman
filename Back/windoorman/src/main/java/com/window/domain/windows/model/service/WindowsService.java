package com.window.domain.windows.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.domain.place.entity.Place;
import com.window.domain.place.repository.PlaceRepository;
import com.window.domain.windows.dto.SensorDataDto;
import com.window.domain.windows.dto.request.WindowsRequestDto;
import com.window.domain.windows.dto.request.WindowsToggleRequestDto;
import com.window.domain.windows.dto.request.WindowsUpdateRequestDto;
import com.window.domain.windows.dto.response.WindowsDetailResponseDto;
import com.window.domain.windows.dto.response.WindowsResponseDto;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
// [수정] 클래스 레벨에 @Transactional(readOnly = true)를 선언하여, 조회 메서드의 성능을 최적화합니다.
@Transactional(readOnly = true)
public class WindowsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WindowsRepository windowsRepository;
    private final PlaceRepository placeRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${smartthings.secret}")
    private String smartThingsSecret;

    @Value("${spring.redis.set.key}")
    private String redisSetKey;

    public Map<String, Object> getWindows(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));

        List<Windows> windows = windowsRepository.findAllByPlaceId(placeId);

        // [수정] N+1 API 호출 문제 해결
        // 각 창문의 상태를 확인하기 위해 반복문 안에서 API를 호출하는 대신, Reactor의 Flux를 사용하여 병렬로 API를 호출하고 결과를 조합합니다.
        // 이를 통해 API 호출에 걸리는 총 대기 시간을 크게 단축시킵니다.
        List<WindowsResponseDto> dtoList = Flux.fromIterable(windows)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(window -> getStateMono(window.getDeviceId())
                        .map(state -> WindowsResponseDto.createResponseDto(window, state)))
                .sequential()
                .collectList()
                .block();

        Map<String, Object> map = new HashMap<>();
        map.put("placeName", place.getName());
        map.put("windows", dtoList);

        return map;
    }

    public WindowsDetailResponseDto getWindowInfo(Long windowsId) {
        // [수정] N+1 문제 해결을 위해 join fetch 쿼리가 적용된 findByIdWithPlace 메서드 사용
        Windows window = windowsRepository.findByIdWithPlace(windowsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        String redisKey = "lastSensorData:" + windowsId;
        Object cachedResult = redisTemplate.opsForValue().get(redisKey);

        SensorDataDto sensorDataDto = (cachedResult instanceof SensorDataDto) ? (SensorDataDto) cachedResult : new SensorDataDto();

        return WindowsDetailResponseDto.builder()
                .placeName(window.getPlace().getName())
                .windowsId(windowsId)
                .name(window.getName())
                .sensorData(sensorDataDto).build();
    }

    @Transactional
    public Long registerWindow(WindowsRequestDto dto) {
        Place place = placeRepository.findById(dto.getPlaceId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));

        // [수정] findByDeviceId가 Optional을 반환하도록 변경되었으므로, ifPresent를 사용하여 중복 체크 로직 개선
        windowsRepository.findByDeviceId(dto.getDeviceId()).ifPresent(w -> {
            throw new CustomException(ErrorCode.DUPLICATE_DEVICEID_EXCEPTION);
        });

        Windows windows = Windows.builder()
                .place(place)
                .name(dto.getName())
                .deviceId(dto.getDeviceId())
                .build();

        return windowsRepository.save(windows).getId();
    }

    @Transactional
    public void updateWindow(WindowsUpdateRequestDto dto) {
        Windows windows = windowsRepository.findById(dto.getWindowsId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        // [수정] 자신을 제외한 다른 창문과 deviceId가 중복되는지 정확히 확인하도록 로직 개선
        windowsRepository.findByDeviceIdAndIdNot(dto.getDeviceId(), dto.getWindowsId()).ifPresent(w -> {
            throw new CustomException(ErrorCode.DUPLICATE_DEVICEID_EXCEPTION);
        });

        windows.updateWindow(dto);
    }

    @Transactional
    public void deleteWindow(Long windowsId) {
        if (!windowsRepository.existsById(windowsId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION);
        }
        windowsRepository.deleteById(windowsId);
    }

    @Transactional
    public void changeToggle(WindowsToggleRequestDto dto) {
        Windows window = windowsRepository.findById(dto.getWindowsId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));
        log.info("isAuto: {}", dto.getIsAuto());
        window.autoUpdateWindow(dto);
    }

    public List<Map<String, Object>> getDevices() {
        String responseJson = webClient.get()
                .header("Content-Type", "application/json")
                .headers(headers -> headers.setBearerAuth(smartThingsSecret))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return parseDeviceData(responseJson);
    }

    // [수정] N+1 DB 조회 문제 해결
    private List<Map<String, Object>> parseDeviceData(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            if (!items.isArray()) return Collections.emptyList();

            List<String> deviceIds = new ArrayList<>();
            items.forEach(item -> deviceIds.add(item.path("deviceId").asText()));

            // [수정] 루프 안에서 DB를 조회하는 대신, deviceId 리스트를 한번에 넘겨 DB에서 조회 (IN 쿼리)
            Set<String> registeredDeviceIds = windowsRepository.findExistingDeviceIds(deviceIds);

            List<Map<String, Object>> parseList = new ArrayList<>();
            for (JsonNode item : items) {
                String deviceId = item.path("deviceId").asText();
                Map<String, Object> map = new HashMap<>();
                map.put("deviceId", deviceId);
                map.put("label", item.path("label").asText());
                // [수정] DB를 다시 조회하지 않고, 미리 조회해온 Set에서 등록 여부 확인
                map.put("isRegistered", registeredDeviceIds.contains(deviceId));
                parseList.add(map);
            }
            return parseList;

        } catch (IOException e) {
            // [수정] e.printStackTrace() 대신 log.error 사용
            log.error("Failed to parse device data from SmartThings API", e);
            return Collections.emptyList();
        }
    }

    // [수정] open/close 로직을 sendCommand 메서드로 통합하여 중복 제거
    public String open(Long windowsId, boolean isAutoAction) {
        Windows windows = windowsRepository.findById(windowsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        if (isAutoAction) {
            if (checkActiveSchedule(windowsId) || !windows.isAuto()) {
                return null;
            }
        }
        return sendCommand(windows.getDeviceId(), "open").block();
    }

    public String close(Long windowsId, boolean isAutoAction) {
        Windows windows = windowsRepository.findById(windowsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));

        if (isAutoAction) {
            if (checkActiveSchedule(windowsId) || !windows.isAuto()) {
                return null;
            }
        }
        return sendCommand(windows.getDeviceId(), "close").block();
    }

    // [추가] 중복되는 API 호출 로직을 공통 메서드로 추출
    private Mono<String> sendCommand(String deviceId, String command) {
        String jsonData = String.format("""
        {
            "commands": [
                {
                    "component": "main",
                    "capability": "windowShade",
                    "command": "%s"
                }
            ]
        }
        """, command);

        return webClient.post()
                .uri("/" + deviceId + "/commands")
                .header("Content-Type", "application/json")
                .headers(headers -> headers.setBearerAuth(smartThingsSecret))
                .bodyValue(jsonData)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Successfully sent command '{}' to deviceId {}: {}", command, deviceId, response))
                .doOnError(e -> log.error("Failed to send command '{}' to deviceId {}", command, deviceId, e));
    }

    public boolean checkActiveSchedule(Long windowsId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisSetKey, String.valueOf(windowsId)));
    }

    public String getState(Long windowsId) {
        Windows window = windowsRepository.findById(windowsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WINDOWS_EXCEPTION));
        return getStateMono(window.getDeviceId()).block();
    }

    // [추가] getWindows에서 병렬 처리를 위해 API 호출을 Mono로 감싸는 메서드
    private Mono<String> getStateMono(String deviceId) {
        return webClient.get()
                .uri("/" + deviceId + "/status")
                .header("Content-Type", "application/json")
                .headers(headers -> headers.setBearerAuth(smartThingsSecret))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseState)
                .onErrorResume(e -> {
                    log.error("Failed to get state for deviceId {}", deviceId, e);
                    return Mono.just("unknown"); // 에러 발생 시 기본 상태 반환
                });
    }

    private String parseState(String json) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            String state = jsonNode.at("/components/main/windowShade/windowShade/value").asText("unknown");
            if (state.contains("open")) return "open";
            if (state.contains("close")) return "close";
            return state;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse state JSON", e);
            return "unknown";
        }
    }
}