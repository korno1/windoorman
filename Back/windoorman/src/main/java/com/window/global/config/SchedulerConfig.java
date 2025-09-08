package com.window.global.config;

import com.window.domain.report.service.ElasticReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SchedulerConfig {

    private final ElasticReportService elasticReportService;
    private final JobLauncher jobLauncher;
    private final Job startTimeJob;
    private final Job endTimeJob;

    // [추가] @Async 문제를 해결하기 위한 자기 자신 주입
    private SchedulerConfig self;

    // [추가] 순환 참조 문제를 피하면서 자기 자신의 프록시 객체를 주입받기 위해 setter에 @Autowired와 @Lazy 사용
    @Autowired
    public void setSelf(@Lazy SchedulerConfig self) {
        this.self = self;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void saveAirReport() {
        try {
            log.info("Scheduler: Saving daily air report.");
            elasticReportService.saveDailyAirReport();
        } catch (Exception e) {
            log.error("Scheduler: Failed to save daily air report.", e);
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void launchJobs() {
        log.info("Scheduler: Launching jobs for current minute.");
        // [수정] this.launch...() 대신, 주입받은 프록시 객체(self)를 통해 메서드를 호출하여 @Async가 정상 동작하도록 수정
        self.launchStartTimeJob();
        self.launchEndTimeJob();
    }

    @Async("asyncExecutor")
    public void launchStartTimeJob() {
        try {
            LocalTime now = LocalTime.now();

            JobParameters startTimeJobParams = new JobParametersBuilder()
                    .addString("startOfMinute", now.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .addString("day", LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN))
                    .addLong("launchTime", System.currentTimeMillis()) // 매번 다른 잡 인스턴스를 위해 추가
                    .toJobParameters();

            log.info("Launching startTimeJob with params: {}", startTimeJobParams);
            jobLauncher.run(startTimeJob, startTimeJobParams);
            log.info("startTimeJob completed successfully.");

        } catch (Exception e) {
            log.error("Error launching startTimeJob", e);
        }
    }

    @Async("asyncExecutor")
    public void launchEndTimeJob() {
        try {
            LocalTime now = LocalTime.now();

            JobParameters endTimeJobParams = new JobParametersBuilder()
                    .addString("redisKey", "schedules:" + now.format(DateTimeFormatter.ofPattern("HH:mm")))
                    .addLong("launchTime", System.currentTimeMillis()) // 매번 다른 잡 인스턴스를 위해 추가
                    .toJobParameters();

            log.info("Launching endTimeJob with params: {}", endTimeJobParams);
            jobLauncher.run(endTimeJob, endTimeJobParams);
            log.info("endTimeJob completed successfully.");

        } catch (Exception e) {
            log.error("Error launching endTimeJob", e);
        }
    }
}