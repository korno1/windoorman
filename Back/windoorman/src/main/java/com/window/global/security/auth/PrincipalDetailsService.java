package com.window.global.security.auth;

import com.window.domain.member.entity.Member;
import com.window.domain.member.repository.MemberRepository;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrincipalDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmailAndIsDelete(email, false)
                .orElseThrow(() -> new ExceptionResponse(ErrorCode.NOT_FOUND_MEMBER_EXCEPTION));
        return new PrincipalDetails(member);
    }
}
