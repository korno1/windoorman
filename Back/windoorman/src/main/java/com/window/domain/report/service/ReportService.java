package com.window.domain.report.service;

import com.window.domain.report.dto.*;
import com.window.domain.report.entity.Graph;
import com.window.domain.report.entity.Report;
import com.window.domain.report.repository.ReportRepository;
import com.window.domain.windowAction.entity.WindowAction;
import com.window.domain.windowAction.repository.WindowActionRepository;
import com.window.domain.windows.entity.Windows;
import com.window.domain.windows.model.repository.WindowsRepository;
import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final WindowActionRepository windowActionRepository;
    private final WindowsRepository windowsRepository;
    private final ElasticsearchOperations operations;

    // [추가] 코드의 의도를 명확히 하기 위해 상수를 정의
    private static final long KST_OFFSET_HOURS = 9; // 한국 시간(KST)은 UTC+9
    private static final DateTimeFormatter ES_INDEX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public ReportResponseDto findAirReport(Long placeId, LocalDate reportDate) {
        Report report = reportRepository.findByPlaceIdAndReportDate(placeId, reportDate)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT_EXCEPTION));

        AirReportDto airReport = AirReportDto.builder()
                .reportId(report.getId())
                .lowTemperature(report.getLowTemperature())
                .highTemperature(report.getHighTemperature())
                .humidity(report.getHumidity())
                .airCondition(report.getAirCondition())
                .build();

        List<WindowsDto> windowsDto = findWindows(placeId);
        // [수정] 로직 결함 수정: 특정 장소의 '첫번째 창문'이 아닌 '모든 창문'의 활동 기록을 조회하도록 변경
        List<ActionsReportResponseDto> actionsReport = findWindowActionsByPlace(placeId, reportDate);

        return ReportResponseDto.builder()
                .airReport(airReport)
                .windows(windowsDto)
                .actionsReport(actionsReport)
                .build();
    }

    public List<ActionsReportResponseDto> findActionsReport(Long windowId, LocalDate reportDate) {
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(LocalTime.MAX);
        // [수정] orElseThrow() 제거. 데이터가 없으면 빈 리스트를 반환하는 것이 더 안정적임.
        List<WindowAction> windowActions = windowActionRepository.findByWindowsIdAndDate(windowId, startOfDay, endOfDay);

        return windowActions.stream()
                .map(action -> ActionsReportResponseDto.builder()
                        .actionReportId(action.getId())
                        // [수정] 하드코딩된 문자열 대신 Enum에 정의된 메서드를 사용하여 가독성 및 유지보수성 향상
                        .open(action.getOpen().getDisplayName())
                        .openTime(action.getOpenTime())
                        .build())
                .collect(Collectors.toList());
    }

    // [추가] findAirReport의 로직 결함을 수정하기 위해 추가된 private 메서드
    private List<ActionsReportResponseDto> findWindowActionsByPlace(Long placeId, LocalDate reportDate) {
        LocalDateTime startOfDay = reportDate.atStartOfDay();
        LocalDateTime endOfDay = reportDate.atTime(LocalTime.MAX);
        List<WindowAction> windowActions = windowActionRepository.findAllByPlaceIdAndDateRange(placeId, startOfDay, endOfDay);

        return windowActions.stream()
                .map(action -> ActionsReportResponseDto.builder()
                        .actionReportId(action.getId())
                        .open(action.getOpen().getDisplayName())
                        .openTime(action.getOpenTime())
                        .build())
                .collect(Collectors.toList());
    }

    public List<WindowsDto> findWindows(Long placeId) {
        // [수정] orElseThrow() 제거. 데이터가 없으면 빈 리스트를 반환하는 것이 더 안정적임.
        List<Windows> windows = windowsRepository.findAllByPlaceId(placeId);
        return windows.stream()
                .map(window -> WindowsDto.builder()
                        .windowsId(window.getId())
                        .name(window.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getLogs(Long actionId) {
        WindowAction action = windowActionRepository.findById(actionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUNT_WINDOWACTION_EXCEPTION));

        Windows windows = action.getWindows();
        LocalDateTime openTime = action.getOpenTime();
        // [수정] UTC 시간으로 변환하는 로직을 상수를 사용하여 명확하게 표현
        LocalDateTime searchStartTime = openTime.minusHours(KST_OFFSET_HOURS + 1);
        LocalDateTime searchEndTime = openTime.minusHours(KST_OFFSET_HOURS);

        String indexName = createIndexName(windows, openTime.toLocalDate());
        // [수정] 불필요한 exists API 호출을 제거하여 성능 개선.
        // Elasticsearch는 존재하지 않는 인덱스를 쿼리에서 자동으로 무시하므로, 두 개의 인덱스 이름을 모두 포함하여 한번에 요청하는 것이 더 효율적입니다.
        if (searchStartTime.toLocalDate().isBefore(openTime.toLocalDate())) {
            String prevIndexName = createIndexName(windows, openTime.toLocalDate().minusDays(1));
            indexName += "," + prevIndexName;
        }

        log.info("Querying Elasticsearch indices: {}", indexName);
        log.info("Time range (UTC): {} to {}", searchStartTime, searchEndTime);

        Criteria criteria = new Criteria("@timestamp").between(searchStartTime, searchEndTime);
        CriteriaQuery query = new CriteriaQuery(criteria).addSort(Sort.by(Sort.Order.asc("@timestamp")));

        List<GraphResponseDto> dtos = operations.search(query, Graph.class, IndexCoordinates.of(indexName))
                .stream()
                .map(SearchHit::getContent)
                .map(e -> GraphResponseDto.builder()
                        .pm10(e.getPm10())
                        .pm25(e.getPm25())
                        .humid(e.getHumid())
                        .temp(e.getTemp())
                        .co2(e.getCo2())
                        .tvoc(e.getTvoc())
                        .timestamp(e.getTimestamp())
                        .isInside(e.getIsInside())
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} graph logs from Elasticsearch.", dtos.size());

        // [수정] 배열 접근 시 발생할 수 있는 예외를 방지하기 위해 길이 체크 로직 추가
        String[] reasonArray = action.getReason().split(",");
        String reason = reasonArray.length > 0 ? reasonArray[0].split("_")[0] : "";

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("reason", reason);
        responseMap.put("data", dtos);

        return responseMap;
    }

    private String createIndexName(Windows windows, LocalDate date) {
        return String.format("%d-%d-%s", windows.getId(), windows.getPlace().getId(), date.format(ES_INDEX_DATE_FORMATTER));
    }
}
