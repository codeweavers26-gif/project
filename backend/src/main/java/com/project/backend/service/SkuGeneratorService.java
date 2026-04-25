package com.project.backend.service;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.backend.repository.ProductVariantRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SkuGeneratorService {

    private final AtomicLong sequence = new AtomicLong(1);
    private final DecimalFormat df = new DecimalFormat("000");

    @Autowired
    private ProductVariantRepository variantRepository;

    /**
     * On startup, read the highest sequence suffix already used in the DB
     * so we never re-use a sequence number after a server restart.
     */
    @PostConstruct
    public void initFromDb() {
        try {
            Long max = variantRepository.findMaxSkuSequence();
            long nextSeq = (max != null && max > 0) ? max + 1 : 1;
            sequence.set(nextSeq);
            log.info("SKU sequence initialized to {} (max in DB was {})", nextSeq, max);
        } catch (Exception e) {
            log.warn("Could not initialize SKU sequence from DB, starting at 1: {}", e.getMessage());
        }
    }

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
