package com.window.domain.place.repository;

import com.window.domain.member.entity.Member;
import com.window.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<List<Place>> findAllByMemberId(Long memberId);

    Optional<Place> findById(Long id);

    Optional<Place> findByIdAndMember(Long id, Member member);

    void deleteById(Long id);
}
