package com.project.backend.service;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class SkuGeneratorService {
    
    private final AtomicLong sequence = new AtomicLong(1);
    private final DecimalFormat df = new DecimalFormat("000");
    
    public String generateSku(String brand, String category, String color, String size) {

        
        String brandCode = brand.toUpperCase().replaceAll("\\s+", "").substring(0, Math.min(5, brand.length()));
        String categoryCode = category.toUpperCase().replaceAll("\\s+", "").substring(0, Math.min(4, category.length()));
        String colorCode = color.toUpperCase().replaceAll("\\s+", "");
        String sizeCode = size.toUpperCase();
        
        long seq = sequence.getAndIncrement();
        String seqStr = df.format(seq);
        
        return String.format("%s-%s-%s-%s-%s", 
            brandCode, categoryCode, colorCode, sizeCode, seqStr);
    }
    
  
    public void initializeSequence(long lastSkuNumber) {
        sequence.set(lastSkuNumber + 1);
    }
}