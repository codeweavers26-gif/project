package com.project.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.AttributeResponseDto;
import com.project.backend.entity.AttributeConfig;
import com.project.backend.entity.AttributeOption;
import com.project.backend.repository.AttributeConfigRepository;
import com.project.backend.repository.AttributeOptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeConfigRepository configRepo;
    private final AttributeOptionRepository optionRepo;

 //   public List<AttributeResponseDto> getAttributesByCategory(Long categoryId) {

//        List<AttributeConfig> configs = configRepo.findByCategoryIdAndActiveTrue(categoryId);
//
//        return configs.stream().map(config -> {
//
//            List<String> options = List.of();
//
//            if ("SELECT".equalsIgnoreCase(config.getInputType())) {
//                options = optionRepo
//                        .findByAttributeIdAndActiveTrueOrderBySortOrder(config.getId())
//                        .stream()
//                        .map(AttributeOption::getValue)
//                        .toList();
//            }
//
//            return AttributeResponseDto.builder()
//                    .name(config.getName())
//                    .required(config.getRequired())
//                    .inputType(config.getInputType())
//                    .options(options)
//                    .build();
//
//        }).toList();
    
//}
}