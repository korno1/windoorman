package com.window.domain.schedule.entity;

import com.window.domain.schedule.dto.request.ScheduleActivateRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ScheduleGroup {

    public ScheduleGroup(LocalDateTime createTime) {

        this.createTime = createTime;
        this.isActivate = true;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @ColumnDefault("true")
    @Column(name = "is_activate", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isActivate;

    public void updateActive(ScheduleActivateRequestDto dto){
        this.isActivate = dto.getIsActivate();
    }

}
