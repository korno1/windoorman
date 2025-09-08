package com.window.domain.windows.model.repository;

import com.window.domain.windows.entity.Windows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WindowsRepository extends JpaRepository<Windows, Long> {

    // [추가] N+1 문제 해결을 위해 Place 정보를 함께 조회하는 join fetch 쿼리
    @Query("SELECT w FROM Windows w JOIN FETCH w.place WHERE w.place.id = :placeId")
    List<Windows> findAllByPlaceIdWithPlace(@Param("placeId") Long placeId);

    // [추가] N+1 문제 해결을 위해 Place 정보를 함께 조회하는 join fetch 쿼리
    @Query("SELECT w FROM Windows w JOIN FETCH w.place WHERE w.id = :id")
    Optional<Windows> findByIdWithPlace(@Param("id") Long id);

    Optional<Windows> findByDeviceId(String deviceId);

    // [추가] 창문 정보 수정 시, 자신을 제외한 다른 창문과의 deviceId 중복을 확인하기 위한 쿼리
    Optional<Windows> findByDeviceIdAndIdNot(String deviceId, Long id);

    // [추가] 여러 deviceId를 한번에 조회하여 DB 쿼리 횟수를 줄이기 위한 쿼리 (N+1 문제 해결)
    @Query("SELECT w.deviceId FROM Windows w WHERE w.deviceId IN :deviceIds")
    Set<String> findExistingDeviceIds(@Param("deviceIds") List<String> deviceIds);

    boolean existsByDeviceId(String deviceId);

    Long countByPlace_Id(Long placeId);

    // [수정] Optional<List<>> 대신 List<>를 반환하도록 하여, 결과가 없을 때 빈 리스트를 반환하도록 수정
    List<Windows> findAllByPlaceId(Long placeId);
}