package com.window.global.security.oauth;

import com.window.domain.member.entity.Member;
import com.window.global.security.auth.PrincipalDetails;
import com.window.global.security.auth.RefreshTokenRepository;
import com.window.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.Cookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Member member = ((PrincipalDetails) authentication.getPrincipal()).getMember();

        String accessToken = jwtTokenProvider.generateAccessToken(member);
        String refreshToken = jwtTokenProvider.generateRefreshToken(member);

        log.info("accessToken: {} refreshToken: {}", accessToken, refreshToken);

        if(refreshTokenRepository.existsRefreshToken(member.getId())){
            refreshTokenRepository.deleteRefreshToken(member.getId());
        }
        refreshTokenRepository.saveRefreshToken(member.getId(), refreshToken);

        String redirectUrl = UriComponentsBuilder.
                fromUriString("https://k11b107.p.ssafy.io/token")
                .queryParam("access", accessToken)
                .build()
                .toString();

        response.addCookie(createCookie("refresh", refreshToken));
        response.setContentType("application/json");
        response.sendRedirect(redirectUrl);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(60*60*60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
