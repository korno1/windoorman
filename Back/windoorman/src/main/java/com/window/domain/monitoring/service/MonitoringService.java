package com.window.domain.monitoring.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.domain.monitoring.dto.GraphDataResponse;
import com.window.domain.monitoring.repository.EmitterRepository;
import com.window.domain.windows.dto.SensorDataDto;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class MonitoringService {
    private final Map<Long, Object> latestDataMap = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmitterRepository emitterRepository;
    private final ElasticsearchClient esClient;
    private static final List<String> CATEGORY_TO_FIELD_LIST = Arrays.asList(
            "",
            "humid",
            "temp",
            "co2",
            "tvoc",
            "pm10",
            "pm25"
    );

    public MonitoringService(RedisTemplate<String, Object> redisTemplate, EmitterRepository emitterRepository, ElasticsearchClient esClient) {
        this.redisTemplate = redisTemplate;
        this.emitterRepository = emitterRepository;
        this.esClient = esClient;
    }

    public SseEmitter subscribe(Long windowId) {
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
//        SseEmitter sseEmitter = new SseEmitter(5000L);
        emitterRepository.save(windowId, sseEmitter);

        log.info("subscribe: " + windowId);
        log.info(String.valueOf(emitterRepository.size()));
        log.info(String.valueOf(emitterRepository.get(windowId)));
        log.info("======================");

        sseEmitter.onCompletion(()-> {
            emitterRepository.deleteById(windowId);
            saveLastSensorDataToRedis(windowId);
            log.info("complete: " + windowId);
        });
        sseEmitter.onTimeout(() -> {
            emitterRepository.deleteById(windowId);
            saveLastSensorDataToRedis(windowId);
            log.info("timeout: " + windowId);
        });
        sseEmitter.onError((e) -> {
            emitterRepository.deleteById(windowId);
            saveLastSensorDataToRedis(windowId);
            log.info("error: " + windowId);
        });


        try {
            sseEmitter.send(SseEmitter.event()
                    .id(String.valueOf(windowId))
                    .name("sensor")
                    .data("{\"message\": \"sensors\"}")
                    .comment("test"));
        } catch (IOException e) {
            emitterRepository.deleteById(windowId);
            sseEmitter.completeWithError(e);
            throw new ExceptionResponse(ErrorCode.FAIL_CONNECT_SSE_EXCEPTION);
        }


        return sseEmitter;

    }

    public void onMessageReceived(Long windowId, Object sensorData) {
        // Cache the latest data locally
        latestDataMap.put(windowId, sensorData);

    }
    public Object getLastSensorData(Long windowId) {
        return latestDataMap.get(windowId);
    }
    private void saveLastSensorDataToRedis(Long windowId) {
        // Fetch the last known sensor data, perhaps from an in-memory cache or a service method
        Object lastSensorData = getLastSensorData(windowId);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonData = objectMapper.writeValueAsString(lastSensorData); // JSON 문자열로 변환
            redisTemplate.opsForValue().set("lastSensorData:" + windowId, jsonData);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("Stored last sensor data for windowId " + windowId);

    }


    public void processSensorData(String payload) {

        String[] pairs = payload.split(":"); // 숫자와 문자 경계에서 split
        List<String> values = new ArrayList<>();

        for (int i = 1; i < pairs.length; i++) { // 값만 추출
            String value = pairs[i].trim().split(" ")[0]; // 공백 이후 문자를 제거하여 값만 남기기
            values.add(value);
        }
        SensorDataDto sensorDataDto = setDto(values);
        log.info(values+"");
        // store last data to redis
        onMessageReceived(sensorDataDto.getWindowsId(), sensorDataDto);

        // send dto to client
        if(values.get(8).equals("1")) {
            sendToClient(sensorDataDto.getWindowsId(), sensorDataDto);
        }
    }
    private SensorDataDto setDto(List<String> values) {
        Long windowId = Long.parseLong(values.get(0));
        Double pm25 = Double.parseDouble(values.get(1));
        Double pm10 = Double.parseDouble(values.get(2));
        Double co2 = Double.parseDouble(values.get(3));
        Double tvoc = Double.parseDouble(values.get(4));
        Double temp = Double.parseDouble(values.get(5));
        Double humid = Double.parseDouble(values.get(6));

        return new SensorDataDto(windowId, co2, tvoc, pm25, pm10, humid, temp);
    }


    public void sendToClient(long windowId, Object data) {
        SseEmitter emitter = emitterRepository.get(windowId);
        if (emitter != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonData = objectMapper.writeValueAsString(data);
                log.info(windowId + ": " +jsonData);

                emitter.send(SseEmitter.event()
                        .id(String.valueOf(windowId))
                        .name("sensor")
                        .data(jsonData));

            } catch (IOException e) {
                emitterRepository.deleteById(windowId);
                emitter.completeWithError(e);
                throw new ExceptionResponse(ErrorCode.FAIL_SEND_SENSORS_EXCEPTION);
            }
        }
    }

    public List<GraphDataResponse> getGraphByRange(Long windowId, int category, int range) {
        switch (range) {
            case 0:
                return getGraphDataDay(windowId, category);
            case 1:
                return getGraphDataLast30Days(windowId, category);
            default:
                return getGraphDataLast365DaysMonthly(windowId, category);
        }


    }

    // get graph data (day)
    private List<GraphDataResponse> getGraphDataDay(Long windowId, int category)  {
        List<GraphDataResponse> responses = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        // 24시간 전과 현재 시간을 구합니다.
        LocalDateTime endDate = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startDate = endDate.minusHours(24);

        // 오늘과 어제 날짜의 인덱스를 생성
        String todayIndex = windowId+"-*-"+endDate.format(formatter);
        String yesterdayIndex = windowId+"-*-"+startDate.format(formatter);


        String fieldName = CATEGORY_TO_FIELD_LIST.get(category);

        try {
            // Multi-index 검색으로 오늘과 어제 인덱스를 동시에 조회
            SearchResponse<Void> search = esClient.search(s -> s
                            .index(todayIndex + "," + yesterdayIndex)
                            .query(q -> q.range(r -> r
                                    .field("@timestamp")
                                    .gte(JsonData.of(startDate.format(timeFormatter))) // 24시간 전
                                    .lte(JsonData.of(endDate.format(timeFormatter)))   // 현재
                            ))
                            .size(0)
                            .aggregations("hourly_avg", a -> a
                                    .dateHistogram(h -> h
                                            .field("@timestamp")
                                            .fixedInterval(Time.of(t -> t.time("1h")))
                                            .format("yyyy-MM-dd HH:mm:ss")
                                    )
                                    .aggregations("avg_value", subAgg -> subAgg.avg(avg -> avg.field(fieldName)))
                            ),
                    Void.class
            );

            DateHistogramAggregate histogram = search.aggregations().get("hourly_avg").dateHistogram();

            for (var bucket : histogram.buckets().array()) {
                String dateStr = bucket.keyAsString();
                assert dateStr != null;
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                double avgValue = bucket.aggregations().get("avg_value").avg().value();
                responses.add(new GraphDataResponse(dateTime, avgValue));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responses;
    }

    public List<GraphDataResponse> getGraphDataLast30Days(Long windowId, int category) {
        List<GraphDataResponse> responses = new ArrayList<>();
        DateTimeFormatter indexFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String fieldName = CATEGORY_TO_FIELD_LIST.get(category);

        List<String> indexNames = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.minusDays(i);
            String indexDate = date.format(indexFormatter);
            indexNames.add(windowId + "-*-" + indexDate);
        }

        try {
            SearchResponse<Void> search = esClient.search(s -> s
                            .index(String.join(",", indexNames))
                            .size(0)
                            .aggregations("daily_avg", a -> a
                                    .dateHistogram(h -> h
                                            .field("@timestamp")
                                            .fixedInterval(Time.of(t -> t.time("1d")))
                                            .format("yyyy-MM-dd")
                                    )
                                    .aggregations("avg_value", subAgg -> subAgg.avg(avg -> avg.field(fieldName)))
                            ),
                    Void.class
            );

            DateHistogramAggregate histogram = search.aggregations().get("daily_avg").dateHistogram();

            for (var bucket : histogram.buckets().array()) {
                String dateStr = bucket.keyAsString();
                LocalDate date = LocalDate.parse(dateStr, dateTimeFormatter);

                double avgValue = bucket.aggregations().get("avg_value").avg().value();
                responses.add(new GraphDataResponse(date.atStartOfDay(), avgValue));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responses;
    }

    public List<GraphDataResponse> getGraphDataLast365DaysMonthly(Long windowId, int category) {
        List<GraphDataResponse> responses = new ArrayList<>();
        DateTimeFormatter indexFormatter = DateTimeFormatter.ofPattern("yyyy.MM");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // fieldName을 CATEGORY_TO_FIELD_LIST에서 가져옴
        String fieldName = CATEGORY_TO_FIELD_LIST.get(category);

        // 이전 365일 동안의 인덱스 이름을 준비
        List<String> indexNames = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        for (int i = 0; i < 12; i++) {
            LocalDate date = today.minusMonths(i);
            String indexDate = date.format(indexFormatter);
            indexNames.add(windowId + "-*-" + indexDate+"*");
        }

        try {
            SearchResponse<Void> search = esClient.search(s -> s
                            .index(String.join(",", indexNames))
                            .size(0)
                            .aggregations("monthly_avg", a -> a
                                    .dateHistogram(h -> h
                                            .field("@timestamp")
                                            .calendarInterval(CalendarInterval.Month)
                                            .format("yyyy-MM")
                                    )
                                    .aggregations("avg_value", subAgg -> subAgg.avg(avg -> avg.field(fieldName)))
                            ),
                    Void.class
            );

            DateHistogramAggregate histogram = search.aggregations().get("monthly_avg").dateHistogram();

            for (var bucket : histogram.buckets().array()) {
                String dateStr = bucket.keyAsString();
                YearMonth yearMonth = YearMonth.parse(dateStr, dateTimeFormatter);
                LocalDate date = yearMonth.atDay(1);
                double avgValue = bucket.aggregations().get("avg_value").avg().value();
                responses.add(new GraphDataResponse(date.atStartOfDay(), avgValue));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return responses;
    }



}
