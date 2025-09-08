package com.window.global.security.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter
@AllArgsConstructor
@Slf4j
public class OAuth2Attributes {

    private String email;
    private String nickname;
    private Map<String, Object> attributes;

    public static OAuth2Attributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        log.info("Kakao account: {}", account);

        return new OAuth2Attributes(
                account.get("email").toString(),
                profile.get("nickname").toString(),
                account
        );
    }
}
