package com.window.global.security.jwt;

import com.window.domain.whitelist.repository.WhitelistRepository;
import com.window.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private JwtTokenProvider jwtTokenProvider;
    private final WhitelistRepository whitelistRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("requestURI: {}", requestURI);

        // 라즈베리파이 whitelist 인증
        String macAddr = request.getHeader("mac");
        log.info("macAddr: {}", macAddr);
        if(macAddr != null && whitelistRepository.existsByMacAddress(macAddr)){
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken("macAddr", null, AuthorityUtils.createAuthorityList("WHITELIST"));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                filterChain.doFilter(request, response);
                return;
        }

        String accessToken = getAccessToken(request);
        if(accessToken != null && jwtTokenProvider.validateAccessToken(accessToken, request)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        request.setAttribute("exception", ErrorCode.NOT_VALID_JWT_EXCEPTION);
        return null;
    }
}
