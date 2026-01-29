package com.project.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.ResponseDto.AttributeResponseDto;
import com.project.backend.service.AttributeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

//    @GetMapping("/{categoryId}/attributes")
//    public List<AttributeResponseDto> getAttributes(@PathVariable Long categoryId) {
//        return attributeService.getAttributesByCategory(categoryId);
//    }
}
