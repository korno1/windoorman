package com.window.domain.report.repository;

import com.window.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByPlaceIdAndReportDate(Long placeId, LocalDate reportDate);
}
