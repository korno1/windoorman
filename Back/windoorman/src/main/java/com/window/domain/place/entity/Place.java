package com.window.domain.place.entity;

import com.window.domain.member.entity.Member;
import com.window.domain.place.dto.PlaceDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter @Builder
@NoArgsConstructor
@AllArgsConstructor
public class Place {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "address", nullable = false, length = 50)
    private String address;

    @Column(name = "detail_address", nullable = false, length = 100)
    private String detailAddress;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    @ColumnDefault("false")
    private Boolean isDefault;

    public void updatePlace(PlaceDto placeDto) {
        if(placeDto.getAddress() != null) this.address = placeDto.getAddress();
        if(placeDto.getDetailAddress() != null) this.detailAddress = placeDto.getDetailAddress();
        if(placeDto.getName() != null) this.name = placeDto.getName();
        this.isDefault = placeDto.getIsDefault();
    }

    public void setDefaultFalse() {
        this.isDefault = false;
    }
}
