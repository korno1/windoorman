package com.window.domain.schedule.model.repository;

import com.window.domain.schedule.entity.Schedule;
import com.window.domain.schedule.entity.ScheduleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // [수정] N+1 문제 해결을 위해 join fetch 적용
    // 연관된 엔티티(ScheduleGroup, Windows, Place)를 한 번의 쿼리로 함께 조회하여 성능을 개선합니다.
    @Query("SELECT s FROM Schedule s " +
           "JOIN FETCH s.scheduleGroup sg " +
           "JOIN FETCH s.windows w " +
           "JOIN FETCH w.place p " +
           "WHERE s.member.id = :memberId")
    List<Schedule> findAllByMemberIdWithDetails(@Param("memberId") Long memberId);

    // [추가] ScheduleService의 update 로직에서 사용하기 위한 메서드
    // 특정 그룹에 속한 모든 스케줄을 한번에 삭제합니다.
    void deleteAllByScheduleGroup(ScheduleGroup scheduleGroup);

    // [추가] ScheduleService의 delete 로직에서 사용하기 위한 메서드
    // 특정 그룹 ID에 속한 모든 스케줄을 한번에 삭제합니다.
    void deleteAllByScheduleGroupId(Long groupId);

    List<Schedule> findByScheduleGroup_Id(Long groupId);

}