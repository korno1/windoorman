package com.window.domain.schedule.model.repository;

import com.window.domain.schedule.entity.ScheduleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleGroupRepository extends JpaRepository<ScheduleGroup, Long> {
}
