package com.window.domain.member.entity;

import com.window.global.security.oauth.OAuth2Attributes;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "nickname", nullable = false,  length = 100)
    private String nickname;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "default_address_id")
    private Long defaultAddressId;

    @ColumnDefault("false")
    @Column(name = "is_delete", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isDelete;

    @Builder
    private Member(String email, String nickname, Role role, Boolean isDelete) {
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.isDelete = isDelete;
        this.defaultAddressId = 0L;
    }

    public static Member signUpMember(OAuth2Attributes oAuth2Attributes){
        return Member.builder()
                .email(oAuth2Attributes.getEmail())
                .nickname(oAuth2Attributes.getNickname())
                .role(Role.ROLE_USER)
                .isDelete(false)
                .build();
    }

    public void updateDefaultAddress(Long defaultAddressId) {
        this.defaultAddressId = defaultAddressId;
    }
}
