package com.window.domain.report.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.window.domain.place.entity.Place;
import com.window.domain.place.repository.PlaceRepository;
import com.window.domain.report.entity.Report;
import com.window.domain.report.repository.ReportRepository;
import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ElasticReportService {

    private final ElasticsearchClient elasticsearchClient;
    private final PlaceRepository placeRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public void saveDailyAirReport() {
        LocalDate reportDate = LocalDate.now().minusDays(1);
        String datePattern = reportDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String indexPattern = "*-" + datePattern;

        // [수정] SchedulerConfig에서 예외를 처리하도록 위임하는 대신, 서비스 내에서 직접 예외를 처리하도록 변경
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(indexPattern)
                    .size(0) // [수정] 집계 결과만 필요하므로, 불필요한 데이터 조회를 막기 위해 size(0) 추가
                    .aggregations("byPlaceIdAgg", a -> a
                            .terms(t -> t.field("placeId"))
                            .aggregations("maxTemp", max -> max.max(m -> m.field("temp")))
                            .aggregations("minTemp", min -> min.min(m -> m.field("temp")))
                            .aggregations("avgHumid", avg -> avg.avg(h -> h.field("humid")))
                            .aggregations("avgPm10", avg -> avg.avg(p -> p.field("pm10")))
                    )
                    .build();

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            Aggregate byPlaceIdAgg = response.aggregations().get("byPlaceIdAgg");

            // [수정] 집계 데이터가 없는 경우, 예외를 던지는 대신 로그를 남기고 정상 종료하여 안정성 향상
            if (byPlaceIdAgg == null || !byPlaceIdAgg.isLterms() || byPlaceIdAgg.lterms().buckets().array().isEmpty()) {
                log.info("No data found in Elasticsearch for index pattern '{}'. Skipping daily report generation.", indexPattern);
                return;
            }

            LongTermsAggregate byPlaceIdTerms = byPlaceIdAgg.lterms();
            for (LongTermsBucket bucket : byPlaceIdTerms.buckets().array()) {
                Long placeId = bucket.key();
                // [수정] 특정 버킷 처리 중 예외가 발생해도 전체 작업이 중단되지 않도록 try-catch 블록 추가
                try {
                    Double maxTemp = bucket.aggregations().get("maxTemp").max().value();
                    Double minTemp = bucket.aggregations().get("minTemp").min().value();
                    Double avgHumid = bucket.aggregations().get("avgHumid").avg().value();
                    Double avgPm10 = bucket.aggregations().get("avgPm10").avg().value();

                    log.info("Aggregated data for placeId {}: MaxTemp={}, MinTemp={}, AvgHumid={}, AvgPm10={}", placeId, maxTemp, minTemp, avgHumid, avgPm10);

                    Place place = placeRepository.findById(placeId)
                            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));

                    Report report = Report.builder()
                            .place(place)
                            .highTemperature(maxTemp)
                            .lowTemperature(minTemp)
                            .humidity(avgHumid)
                            .airCondition(avgPm10)
                            .reportDate(reportDate)
                            .build();
                    reportRepository.save(report);
                } catch (Exception e) {
                    log.error("Failed to process and save report for placeId: {}", placeId, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to execute Elasticsearch query for daily report generation.", e);
        }
    }
}
