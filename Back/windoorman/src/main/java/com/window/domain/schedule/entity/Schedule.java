package com.window.domain.schedule.entity;

import com.window.domain.member.entity.Member;
import com.window.domain.schedule.dto.request.ScheduleActivateRequestDto;
import com.window.domain.schedule.dto.request.ScheduleUpdateRequestDto;
import com.window.domain.windows.entity.Windows;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
public class Schedule {

    @Builder
    public Schedule(Long id, ScheduleGroup scheduleGroup, Windows windows, Member member, LocalTime startTime, LocalTime endTime, Day day, boolean isActivate) {
        this.id = id;
        this.scheduleGroup = scheduleGroup;
        this.windows = windows;
        this.member = member;
        this.startTime = startTime;
        this.endTime = endTime;
        this.day = day;
        this.isActivate = isActivate;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ScheduleGroup scheduleGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "windows_id")
    private Windows windows;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Day day;

    @ColumnDefault("false")
    @Column(name = "is_activate", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isActivate;




}
