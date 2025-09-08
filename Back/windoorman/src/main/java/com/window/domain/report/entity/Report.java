package com.window.domain.report.entity;

import com.window.domain.place.entity.Place;
import com.window.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
public class Report extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "low_temperature", nullable = false)
    private Double lowTemperature;

    @Column(name = "high_temperature", nullable = false)
    private Double highTemperature;

    @Column(name = "humidity", nullable = false)
    private Double humidity;

    @Column(name = "air_condition", nullable = false)
    private Double airCondition;

    @CreatedDate
    @Column(name = "report_date", nullable = false, updatable = false)
    private LocalDate reportDate;

    private Report(Long id, Place place, Double lowTemperature, Double highTemperature, Double humidity, Double airCondition, LocalDate reportDate) {
        this.id = id;
        this.place = place;
        this.lowTemperature = lowTemperature;
        this.highTemperature = highTemperature;
        this.humidity = humidity;
        this.airCondition = airCondition;
        this.reportDate = reportDate;
    }
}
