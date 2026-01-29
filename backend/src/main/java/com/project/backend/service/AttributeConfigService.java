package com.project.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.entity.AttributeConfig;
import com.project.backend.repository.AttributeConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttributeConfigService {

    private final AttributeConfigRepository repository;

    public List<AttributeConfig> getActiveAttributes() {
        return repository.findByActiveTrue();
    }
}

