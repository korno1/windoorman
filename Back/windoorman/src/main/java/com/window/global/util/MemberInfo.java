package com.window.global.util;

import com.window.domain.member.entity.Member;
import com.window.global.security.auth.PrincipalDetails;
import org.springframework.security.core.Authentication;

public class MemberInfo {
    public static Member getMemberInfo(Authentication authentication) {

        return ((PrincipalDetails) authentication.getPrincipal()).getMember();
    }
}
