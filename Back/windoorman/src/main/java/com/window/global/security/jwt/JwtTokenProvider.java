package com.window.global.security.jwt;

import com.window.domain.member.entity.Member;
import com.window.global.config.JwtValueConfig;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import com.window.global.security.auth.PrincipalDetails;
import com.window.global.security.auth.PrincipalDetailsService;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtValueConfig config;
    private final PrincipalDetailsService principalDetailsService;

    public String generateAccessToken(Member member) {
        return Jwts.builder()
                .claim("id", member.getId())
                .claim("email", member.getEmail())
                .setExpiration(expireDate(config.getAccessExpirationTime()))
                .signWith(config.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Member member) {
        return Jwts.builder()
                .claim("id", member.getId())
                .claim("email", member.getEmail())
                .setExpiration(expireDate(config.getRefreshExpirationTime()))
                .signWith(config.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Date expireDate(long expireTime) {
        return new Date(new Date().getTime() + expireTime);
    }

    public boolean validateAccessToken(String token, HttpServletRequest request) {
        try {
            Jwts.parser()
                    .setSigningKey(config.getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return  true;
        } catch (ExpiredJwtException e){
            request.setAttribute("exception", ErrorCode.EXPIRED_JWT_EXCEPTION);
        } catch (JwtException e){
            request.setAttribute("exception", ErrorCode.NOT_VALID_JWT_EXCEPTION);
        }
        return false;
    }

    public void validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(config.getSecretKey())
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e){
            throw new ExceptionResponse(ErrorCode.EXPIRED_JWT_EXCEPTION);
        } catch (JwtException e){
            throw new ExceptionResponse(ErrorCode.NOT_VALID_JWT_EXCEPTION);
        }
    }

    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(config.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    public Authentication getAuthentication(String token) {
        String memberEmail = getEmail(token);
        PrincipalDetails principalDetails = (PrincipalDetails) principalDetailsService.loadUserByUsername(memberEmail);
        return new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
    }
}
