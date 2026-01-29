package com.project.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.entity.AttributeConfig;
import com.project.backend.service.AttributeConfigService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeConfigController {

    private final AttributeConfigService service;
//
//    @GetMapping
//    public List<AttributeConfig> getAttributesForProductCreate() {
//        return service.getActiveAttributes();
//    }
}
