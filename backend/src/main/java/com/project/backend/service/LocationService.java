package com.project.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Comparator;
import com.project.backend.ResponseDto.LocationResponseDto;
import com.project.backend.ResponseDto.ServiceabilityResponse;
import com.project.backend.config.ShippingFactory;
import com.project.backend.entity.Location;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.ShippingProviderType;
import com.project.backend.entity.UserAddress;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.LocationRequestDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
   private final ShippingFactory shippingFactory;
   
    private final RestTemplate restTemplate;
    private final ShiprocketService shiprocketService;
    private final LocationRepository locationRepository;
    
	private final UserAddressRepository userAddressRepository;
	private final UserRepository userRepository;

    @CacheEvict(value = "activeLocations", allEntries = true)
    public Location createLocation(LocationRequestDto dto) {

        if (locationRepository.existsByCityAndPincode(dto.getCity(), dto.getPincode())) {
            throw new BadRequestException("Location already exists");
        }

        Location location = Location.builder()
                .name(dto.getName())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .deliveryDays(dto.getDeliveryDays())
                .codAvailable(dto.getCodAvailable())
                .extraShippingCharge(dto.getExtraShippingCharge())
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
                        .name(loc.getName())
                        .city(loc.getCity())
                        .state(loc.getState())
                        .pincode(loc.getPincode())
                        .deliveryDays(loc.getDeliveryDays())
                        .codAvailable(loc.getCodAvailable())
                        .build())
                .toList();
    }

    public Location updateLocation(Long id, LocationRequestDto dto) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        location.setName(dto.getName());
        location.setCity(dto.getCity());
        location.setState(dto.getState());
        location.setPincode(dto.getPincode());
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setDeliveryDays(dto.getDeliveryDays());
        location.setCodAvailable(dto.getCodAvailable());
        location.setExtraShippingCharge(dto.getExtraShippingCharge());

        return locationRepository.save(location);
    }

    public void disableLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        location.setIsActive(false);
        locationRepository.save(location);
    }

    public Location findServiceableLocation(String pincode) {
        return locationRepository.findFirstByPincodeAndIsActiveTrue(pincode)
                .orElseThrow(() -> new NotFoundException("Delivery not available at this pincode"));
    }

    public ServiceabilityResponse checkServicibility( String pincode){

        ServiceabilityResponse serviceability =
        shippingFactory.getProvider(ShippingProviderType.SHIPROCKET)
                .checkServiceability(
                        "110001", 
                        pincode,
                        0.5,
                       true
                );

if (!serviceability.isServiceable()) {
    throw new BadRequestException("Delivery not available for this pincode");
}
ServiceabilityResponse.CourierOption bestCourier =
        serviceability.getCouriers().stream()
                .min(Comparator
                        .comparingInt(ServiceabilityResponse.CourierOption::getDeliveryDays)
                        .thenComparingDouble(ServiceabilityResponse.CourierOption::getRate)
                )
                .orElseThrow();

    return ServiceabilityResponse.builder()
            .serviceable(true)
            .couriers(List.of(bestCourier))
            .build();

    }
}
