package com.project.backend.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.LocationResponseDto;
import com.project.backend.entity.Location;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.requestDto.LocationRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    @CacheEvict(value = "activeLocations", allEntries = true)
    public Location createLocation(LocationRequestDto dto) {

        if (locationRepository.existsByCityAndPincode(
                dto.getCity(), dto.getPincode())) {
            throw new BadRequestException("Location already exists");
        }

        Location location = Location.builder()
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .isActive(true)
                .build();

        return locationRepository.save(location);
    }
    
    @Cacheable("activeLocations")
    public List<LocationResponseDto> getActiveLocations() {

        return locationRepository.findByIsActiveTrue()
                .stream()
                .map(loc -> LocationResponseDto.builder()
                        .id(loc.getId())
                        .city(loc.getCity())
                        .state(loc.getState())
                        .pincode(loc.getPincode())
                        .build())
                .toList();
    }
    public Location updateLocation(Long id, LocationRequestDto dto) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        location.setCity(dto.getCity());
        location.setState(dto.getState());
        location.setPincode(dto.getPincode());

        return locationRepository.save(location);
    }

    public void disableLocation(Long id) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        location.setIsActive(false);
        locationRepository.save(location);
    }

}
