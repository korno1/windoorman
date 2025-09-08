package com.window.domain.place.controller;

import com.window.domain.place.dto.GetPlacesResponseDto;
import com.window.domain.place.dto.PlaceDto;
import com.window.domain.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<?> getPlaces(Authentication authentication) {
        GetPlacesResponseDto responseDto = placeService.getPlaces(authentication);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<?> registerPlace(@RequestBody PlaceDto place, Authentication authentication) {
        Long registeredPlaceId = placeService.registPlace(place, authentication);
        return ResponseEntity.ok(registeredPlaceId);
    }

    @PatchMapping
    public ResponseEntity<?> updatePlace(@RequestBody PlaceDto place, Authentication authentication) {
        Long registeredPlaceId = placeService.updatePlace(place, authentication);
        return ResponseEntity.ok(registeredPlaceId);
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<?> deletePlace(@PathVariable("placeId") Long placeId, Authentication authentication) {
        Long deletedPlaceId = placeService.deletePlace(placeId);
        return ResponseEntity.ok(deletedPlaceId);
    }

}