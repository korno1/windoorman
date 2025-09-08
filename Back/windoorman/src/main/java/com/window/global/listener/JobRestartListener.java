package com.window.global.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class JobRestartListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} started with parameters: {}", jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("Job {} failed.", jobExecution.getJobInstance().getJobName());

            String jobName = jobExecution.getJobInstance().getJobName();
            JobParameters jobParameters = jobExecution.getJobParameters();

            // 재시작 명령어를 구성합니다.
            // 실제 환경에 맞게 java -jar your-app.jar 부분은 수정해야 합니다.
            String restartCommand = "java -jar your-app.jar --spring.batch.job.names=" + jobName;

            // JobParameters를 커맨드 라인 인자로 변환합니다.
            String paramsString = jobParameters.getParameters().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("launchTime")) // launchTime은 재시작 시 제외
                    .map(entry -> {
                        JobParameter param = entry.getValue();
                        return entry.getKey() + "=" + param.getValue() + "(" + param.getType() + ")";
                    })
                    .collect(Collectors.joining(" "));

            if (!paramsString.isEmpty()) {
                restartCommand += " " + paramsString;
            }

            log.error("To restart this job, use the following command: {}", restartCommand);
        } else if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job {} completed successfully.", jobExecution.getJobInstance().getJobName());
        }
    }
}
