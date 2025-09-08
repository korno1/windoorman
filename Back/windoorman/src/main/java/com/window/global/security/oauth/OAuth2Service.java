package com.window.global.security.oauth;

import com.window.domain.member.entity.Member;
import com.window.domain.member.repository.MemberRepository;
import com.window.global.exception.CustomException;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import com.window.global.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2Attributes oAuth2Attributes = OAuth2Attributes.ofKakao(attributes);

        if(!memberRepository.existsByEmailAndIsDelete(oAuth2Attributes.getEmail(), false)) {
            signUpOAuthMember(oAuth2Attributes);
        }

        Member member = memberRepository.findByEmailAndIsDelete(oAuth2Attributes.getEmail(), false)
                .orElseThrow(() -> new ExceptionResponse(ErrorCode.NOT_FOUND_MEMBER_EXCEPTION));

        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }

    private void signUpOAuthMember(OAuth2Attributes oAuth2Attributes) {
        Member signUpMember = Member.signUpMember(oAuth2Attributes);
        log.info("signUpMember : {}", signUpMember);
        memberRepository.save(signUpMember);
    }
}
