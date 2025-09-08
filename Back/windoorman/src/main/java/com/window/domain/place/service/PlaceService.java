package com.window.domain.place.service;

import com.window.domain.member.entity.Member;
import com.window.domain.member.repository.MemberRepository;
import com.window.domain.place.dto.GetPlacesResponseDto;
import com.window.domain.place.dto.PlaceDto;
import com.window.domain.place.entity.Place;
import com.window.domain.place.repository.PlaceRepository;
import com.window.global.exception.ErrorCode;
import com.window.global.exception.ExceptionResponse;
import com.window.global.util.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    public GetPlacesResponseDto getPlaces(Authentication authentication) {
        Member member = MemberInfo.getMemberInfo(authentication);
        List<Place> places = placeRepository.findAllByMemberId(member.getId()).orElseThrow(() ->new ExceptionResponse(ErrorCode.NOT_FOUND_MEMBER_EXCEPTION));

        List<PlaceDto> placeDtos = new ArrayList<>();

        for(Place place: places) {
            placeDtos.add(new PlaceDto(place.getId(), place.getName(), place.getAddress(), place.getDetailAddress(), place.getIsDefault()));
        }

        return new GetPlacesResponseDto(member.getNickname(), placeDtos);
    }

    public Long registPlace(PlaceDto placeDto, Authentication authentication) {
        Member member = MemberInfo.getMemberInfo(authentication);

        Place place = Place.builder()
                .name(placeDto.getName())
                .address(placeDto.getAddress())
                .detailAddress(placeDto.getDetailAddress())
                .member(member)
                .isDefault(placeDto.getIsDefault())
                .build();

        place = placeRepository.save(place);

        Long beforeDefaultId = member.getDefaultAddressId();


        if(placeDto.getIsDefault() ) {
            // 원래 디폴트 주소를 false
            if(beforeDefaultId!=0) {
                Place beforeDefault = placeRepository.findById(beforeDefaultId).orElseThrow(() -> new ExceptionResponse(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));
                beforeDefault.setDefaultFalse();
            }
            // 멤버 디폴트 주소 변경
            member.updateDefaultAddress(place.getId());
        }

        memberRepository.save(member);
        return place.getId();
    }

    public Long updatePlace(PlaceDto placeDto, Authentication authentication ) {

        Place place = placeRepository.findById(placeDto.getId()).orElseThrow(()->new ExceptionResponse(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));
        place.updatePlace(placeDto);
        placeRepository.save(place);

        Member member = MemberInfo.getMemberInfo(authentication);
        Long beforeDefaultId = member.getDefaultAddressId();
        if(placeDto.getIsDefault() ) {
            // 원래 디폴트 주소를 false
            if(beforeDefaultId!=0) {
                Place beforeDefault = placeRepository.findById(beforeDefaultId).orElseThrow(() -> new ExceptionResponse(ErrorCode.NOT_FOUND_PLACE_EXCEPTION));
                beforeDefault.setDefaultFalse();
            }
            // 멤버 디폴트 주소 변경
            member.updateDefaultAddress(place.getId());
        }
        memberRepository.save(member);
        return place.getId();
    }

    public Long deletePlace(Long placeId) {
        placeRepository.deleteById(placeId);

        return placeId;
    }


}
