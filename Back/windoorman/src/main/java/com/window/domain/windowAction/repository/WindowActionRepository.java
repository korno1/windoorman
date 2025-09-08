package com.window.domain.windowAction.repository;

import com.window.domain.windowAction.entity.WindowAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WindowActionRepository extends JpaRepository<WindowAction, Long> {

    // [수정] Optional<List<>> 대신 List<>를 반환하도록 하여, 결과가 없을 때 빈 리스트를 반환하도록 수정
    @Query("SELECT wa " +
            "FROM WindowAction wa " +
            "WHERE wa.windows.id = :windowsId " +
            "AND wa.openTime >= :startOfDay " +
            "AND wa.openTime < :endOfDay")
    List<WindowAction> findByWindowsIdAndDate(
            @Param("windowsId") Long windowsId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    // [추가] ReportService에서 특정 장소의 모든 액션 기록을 조회하기 위한 메서드
    @Query("SELECT wa FROM WindowAction wa JOIN wa.windows w WHERE w.place.id = :placeId AND wa.openTime >= :startOfDay AND wa.openTime < :endOfDay")
    List<WindowAction> findAllByPlaceIdAndDateRange(@Param("placeId") Long placeId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);


    @Query("SELECT COUNT(wa) " +
            "FROM WindowAction wa " +
            "INNER JOIN wa.windows w " +
            "WHERE w.place.id = :placeId " +
            "AND wa.openTime >= :startOfDay " +
            "AND wa.openTime < :endOfDay")
    Long countByCountAction(
            @Param("placeId") Long placeId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

}