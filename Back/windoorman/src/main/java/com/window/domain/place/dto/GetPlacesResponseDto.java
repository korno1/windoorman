package com.window.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class GetPlacesResponseDto {
    private String nickname;
    private List<PlaceDto> placeDtoList;
}
