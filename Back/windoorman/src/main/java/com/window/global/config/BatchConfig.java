package com.window.global.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.domain.schedule.dto.ScheduleRedisDto;
import com.window.domain.schedule.entity.Schedule;
import com.window.domain.windows.entity.Windows;
import com.window.global.listener.CustomSkipListener;
import com.window.global.listener.JobRestartListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final WebClient webClient;
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, ScheduleRedisDto> scheduleRedisTemplate;
    private final ThreadPoolTaskExecutor asyncExecutor;
    private final JobRestartListener jobRestartListener; // [추가] JobRestartListener 주입
    private final CustomSkipListener customSkipListener;


    @Value("${smartthings.secret}")
    private String smartThingsSecret;

    @Value("${spring.redis.set.key}")
    private String redisSetKey;

    // [수정] 반복되는 문자열을 상수로 선언하여 재사용성 및 유지보수성 향상
    private static final String START_TIME_JOB = "startTimeJob";
    private static final String START_TIME_STEP = "startTimeStep";
    private static final String END_TIME_JOB = "endTimeJob";
    private static final String END_TIME_STEP = "endTimeStep";
    private static final String OPEN_COMMAND_JSON = """
            {
                "commands": [
                    {
                        "component": "main",
                        "capability": "windowShade",
                        "command": "open"
                    }
                ]
            }
            """;
    private static final String CLOSE_COMMAND_JSON = """
            {
                "commands": [
                    {
                        "component": "main",
                        "capability": "windowShade",
                        "command": "close"
                    }
                ]
            }
            """;


    @Bean
    public Job startTimeJob() {
        return new JobBuilder(START_TIME_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(startTimeStep())
                .listener(jobRestartListener) // [추가] JobRestartListener 등록
                .build();
    }

    @Bean
    public Step startTimeStep() {
        return new StepBuilder(START_TIME_STEP, jobRepository)
                .<Schedule, Schedule>chunk(100, transactionManager)
                // [수정] JobParameter를 사용하기 위해 Reader를 지연 로딩(Lazy Binding) 방식으로 변경
                .reader(startTimeReader(null, null))
                .writer(startTimeWriter())
                .taskExecutor(asyncExecutor)
                .allowStartIfComplete(true)
                // [추가] 내결함성(Fault Tolerance) 기능 활성화 및 재시도/건너뛰기 정책 정의
                .faultTolerant() // 내결함성 기능 활성화
                .retryLimit(3)   // 최대 3번까지 재시도
                .retry(IOException.class) // I/O 오류 재시도
                .retry(SocketTimeoutException.class) // 소켓 타임아웃 오류 재시도
                .retry(WebClientResponseException.class) // WebClient 응답 오류 재시도 (5xx 등)
                .skipLimit(10)   // 최대 10개의 아이템 건너뛰기
                .skip(JsonProcessingException.class) // JSON 처리 오류 건너뛰기
                .skip(IllegalArgumentException.class) // 잘못된 인자 오류 건너뛰기
                .noRollback(JsonProcessingException.class) // 건너뛴 오류 발생 시 롤백하지 않음
                .noRollback(IllegalArgumentException.class)
                .listener(customSkipListener) // 건너뛴 오류 발생 시 롤백하지 않음
                .build();
    }


    @Bean
    @StepScope // [수정] JobParameter를 받기 위해 @StepScope 추가
    public JdbcPagingItemReader<Schedule> startTimeReader(@Value("#{jobParameters['startOfMinute']}") String startOfMinuteStr,
                                                          @Value("#{jobParameters['day']}") String day) {
        // [수정] BETWEEN 절을 위한 시간 파라미터 계산
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startOfMinute = LocalTime.parse(startOfMinuteStr, formatter);
        Time startTime = Time.valueOf(startOfMinute);
        Time endTime = Time.valueOf(startOfMinute.plusSeconds(59));

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("s.id", Order.ASCENDING);

        Map<String, Object> params = new HashMap<>();
        // [수정] 계산된 시간 파라미터와 요일 추가
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("day", day);

        log.info("startTimeReader executed with params: startTime={}, endTime={}, day={}", startTime, endTime, day);

        return new JdbcPagingItemReaderBuilder<Schedule>()
                .name("jdbcPagingItemReader")
                .dataSource(dataSource)
                .fetchSize(100)
                .selectClause("SELECT s.id, s.start_time, s.end_time, w.id AS windows_id, w.device_id")
                .fromClause("FROM schedule s LEFT JOIN windows w ON s.windows_id = w.id " +
                        "JOIN schedule_group sg ON s.group_id = sg.id")
                // [수정] WHERE 절을 함수 사용 대신 BETWEEN으로 변경하여 인덱스 활용
                .whereClause("WHERE sg.is_activate = true AND s.start_time BETWEEN :startTime AND :endTime AND s.day = :day")
                .sortKeys(sortKeys)
                .parameterValues(params)
                .rowMapper((rs, rowNum) -> {
                    Windows window = Windows.builder()
                            .id(rs.getLong("windows_id"))
                            .deviceId(rs.getString("device_id"))
                            .build();

                    return Schedule.builder()
                            .id(rs.getLong("id"))
                            .startTime(rs.getTime("start_time").toLocalTime())
                            .endTime(rs.getTime("end_time").toLocalTime())
                            .windows(window)
                            .build();
                })
                .build();
    }

    @Bean
    public ItemWriter<Schedule> startTimeWriter() {
        return items -> {
            for (Schedule schedule : items) {
                log.info("Processing scheduleId: {}", schedule.getId());
                Windows window = schedule.getWindows();
                redisTemplate.opsForSet().add(redisSetKey, String.valueOf(window.getId()));
                String deviceId = window.getDeviceId();

                try {
                    // [수정] 비동기 non-blocking 호출(.subscribe())을 동기 blocking 호출(.block())으로 변경하여 API 호출이 완료될 때까지 대기하도록 수정
                    // 이를 통해 API 호출 실패 시 배치가 정상적으로 실패하고 재시도 로직이 동작하도록 보장
                    String response = webClient.post()
                            .uri("/" + deviceId + "/commands")
                            .header("Content-Type", "application/json")
                            .headers(headers -> headers.setBearerAuth(smartThingsSecret))
                            .bodyValue(OPEN_COMMAND_JSON)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    log.info("Open command response: {}", response);
                } catch (Exception e) {
                    // [수정] e.printStackTrace() 대신 log.error를 사용하여 에러 로그를 명확히 기록
                    log.error("Failed to send open command for deviceId: {}", deviceId, e);
                    throw e;
                }
            }

            Map<LocalTime, List<Schedule>> schedulesByEndTime = items.getItems().stream()
                    .collect(Collectors.groupingBy(Schedule::getEndTime));

            schedulesByEndTime.forEach((endTime, schedules) -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                String redisKey = "schedules:" + endTime.format(formatter);

                List<ScheduleRedisDto> redisDtos = schedules.stream()
                        .map(schedule -> new ScheduleRedisDto(schedule.getWindows().getId(), endTime, schedule.getWindows().getDeviceId()))
                        .toList();

                redisDtos.forEach(dto -> {
                    try {
                        scheduleRedisTemplate.opsForSet().add(redisKey, dto);
                        log.info("Saved to Redis key {}: {}", redisKey, objectMapper.writeValueAsString(dto));
                    } catch (JsonProcessingException e) {
                        // [수정] e.printStackTrace() 대신 log.error를 사용하여 에러 로그를 명확히 기록
                        log.error("Failed to serialize ScheduleRedisDto for redis key: {}", redisKey, e);
                    }
                });
            });
        };
    }

    @Bean
    public Job endTimeJob() {
        return new JobBuilder(END_TIME_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(endTimeStep())
                .listener(jobRestartListener) // [추가] JobRestartListener 등록
                .build();
    }

    @Bean
    public Step endTimeStep() {
        return new StepBuilder(END_TIME_STEP, jobRepository)
                .<ScheduleRedisDto, ScheduleRedisDto>chunk(100, transactionManager)
                // [수정] JobParameter를 사용하기 위해 Reader를 지연 로딩(Lazy Binding) 방식으로 변경
                .reader(endTimeReader(null))
                .writer(endTimeWriter())
                .taskExecutor(asyncExecutor)
                .allowStartIfComplete(true)
                // [추가] 내결함성(Fault Tolerance) 기능 활성화 및 재시도/건너뛰기 정책 정의
                .faultTolerant() // 내결함성 기능 활성화
                .retryLimit(3)   // 최대 3번까지 재시도
                .retry(IOException.class) // I/O 오류 재시도
                .retry(SocketTimeoutException.class) // 소켓 타임아웃 오류 재시도
                .retry(WebClientResponseException.class) // WebClient 응답 오류 재시도 (5xx 등)
                .skipLimit(10)   // 최대 10개의 아이템 건너뛰기
                .skip(JsonProcessingException.class) // JSON 처리 오류 건너뛰기
                .skip(IllegalArgumentException.class) // 잘못된 인자 오류 건너뛰기
                .noRollback(JsonProcessingException.class) // 건너뛴 오류 발생 시 롤백하지 않음
                .noRollback(IllegalArgumentException.class)
                .listener(customSkipListener) // 건너뛴 오류 발생 시 롤백하지 않음
                .build();
    }

    @Bean
    @StepScope // [수정] JobParameter를 받고, 상태를 가지지 않는 Reader를 매번 새로 생성하기 위해 @StepScope 추가
    public ListItemReader<ScheduleRedisDto> endTimeReader(@Value("#{jobParameters['redisKey']}") String redisKey) {
        log.info("endTimeReader executed for redisKey: {}", redisKey);
        Set<ScheduleRedisDto> dtoList = scheduleRedisTemplate.opsForSet().members(redisKey);
        if (dtoList == null || dtoList.isEmpty()) {
            return new ListItemReader<>(Collections.emptyList());
        }
        return new ListItemReader<>(new ArrayList<>(dtoList));
    }


    @Bean
    @StepScope // [수정] JobParameter(redisKey)를 사용하지는 않지만, writer도 StepScope로 만들어 일관성 유지
    public ItemWriter<ScheduleRedisDto> endTimeWriter() {
        return items -> {
            String redisKey = null;
            for (ScheduleRedisDto dto : items) {
                if (redisKey == null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    redisKey = "schedules:" + dto.getEndTime().format(formatter);
                }

                redisTemplate.opsForSet().remove(redisSetKey, String.valueOf(dto.getWindowsId()));
                String deviceId = dto.getDeviceId();
                try {
                    // [수정] 비동기 non-blocking 호출(.subscribe())을 동기 blocking 호출(.block())으로 변경
                    String response = webClient.post()
                            .uri("/" + deviceId + "/commands")
                            .header("Content-Type", "application/json")
                            .headers(headers -> headers.setBearerAuth(smartThingsSecret))
                            .bodyValue(CLOSE_COMMAND_JSON)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();
                    log.info("Close command response: {}", response);
                } catch (Exception e) {
                    log.error("Failed to send close command for deviceId: {}", deviceId, e);
                    throw e;
                }
            }
            // [수정] 데이터 유실 방지를 위해 Reader가 아닌 Writer에서 모든 작업이 성공적으로 끝난 후 Redis 키를 삭제하도록 로직 이동
            if (redisKey != null) {
                log.info("Deleting redis key: {}", redisKey);
                scheduleRedisTemplate.delete(redisKey);
            }
        };
    }
}
