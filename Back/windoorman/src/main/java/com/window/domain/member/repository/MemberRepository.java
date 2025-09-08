package com.window.domain.member.repository;

import com.window.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndIsDelete(String email, Boolean isDelete);
    Boolean existsByEmailAndIsDelete(String email, Boolean isDelete);
}
