package com.window.domain.member.service;

import com.window.domain.member.entity.Member;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import com.window.global.security.auth.PrincipalDetails;
import com.window.global.security.auth.RefreshTokenRepository;
import com.window.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReissueService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public String reissue(HttpServletRequest request, HttpServletResponse response){
        Cookie[] cookies = request.getCookies();
        log.info("cookies: {}", Arrays.toString(cookies));
        if(cookies == null){
            throw new ExceptionResponse(ErrorCode.NOT_FOUND_REFRESH_EXCEPTION);
        }

        String cookieRefresh = getRefreshTokenFromCookies(cookies);
        log.info("cookieRefresh: {}", cookieRefresh);

        jwtTokenProvider.validateRefreshToken(cookieRefresh);
        Authentication authentication = jwtTokenProvider.getAuthentication(cookieRefresh);
        Member member = ((PrincipalDetails) authentication.getPrincipal()).getMember();

        String redisRefresh = refreshTokenRepository.getRefreshToken(member.getId());
        log.info("redisRefresh: {}", redisRefresh);

        if(!cookieRefresh.equals(redisRefresh)){
            throw new ExceptionResponse(ErrorCode.NOT_FOUND_REFRESH_EXCEPTION);
        }

        return jwtTokenProvider.generateAccessToken(member);
    }

    private String getRefreshTokenFromCookies(Cookie[] cookies) {
        return Arrays.stream(cookies)
                .filter(cookie -> "refresh".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new ExceptionResponse(ErrorCode.NOT_FOUND_REFRESH_EXCEPTION));
    }
}
