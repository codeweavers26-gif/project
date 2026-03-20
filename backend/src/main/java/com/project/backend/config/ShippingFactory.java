package com.project.backend.config;

import org.springframework.stereotype.Service;

import com.project.backend.entity.ShippingProviderType;
import com.project.backend.service.ShiprocketService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShippingFactory {

    private final ShiprocketService shiprocketService;

    public ShippingProvider getProvider(ShippingProviderType type) {

        return switch (type) {
            case SHIPROCKET -> shiprocketService;
          
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        };
    }
}