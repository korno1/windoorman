package com.window.domain.windowAction.entity;


import com.window.domain.windowAction.dto.ReasonDto;
import com.window.domain.windowAction.dto.request.WindowActionRequestDto;
import com.window.domain.windows.entity.Windows;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor
public class WindowAction {

    @Builder
    public WindowAction(Windows windows, Open open, LocalDateTime openTime, String reason, String status) {
        this.windows = windows;
        this.open = open;
        this.openTime = openTime;
        this.reason = reason;
        this.status = status;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "windows_id")
    private Windows windows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Open open;

    @Column(name = "open_time", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime openTime;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "status", nullable = false, length = 500)
    private String status;

    // [추가] DTO에서 Entity를 생성하는 정적 팩토리 메서드 (static factory method)
    // 서비스 레이어의 DTO -> Entity 변환 로직을 엔티티로 옮겨, 서비스 코드의 책임을 덜어주고 응집도를 높입니다.
    public static WindowAction of(WindowActionRequestDto dto, Windows windows) {
        String sensors = dto.getReason().stream()
                .map(ReasonDto::getSensor)
                .collect(Collectors.joining(","));

        String statuses = dto.getReason().stream()
                .map(ReasonDto::getStatus)
                .collect(Collectors.joining(","));

        return WindowAction.builder()
                .windows(windows)
                .open(dto.getOpen())
                .openTime(dto.getOpenTime())
                .reason(sensors)
                .status(statuses)
                .build();
    }

}