package com.project.backend.service;

import org.springframework.stereotype.Service;

import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.WarehouseInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final WarehouseInventoryRepository inventoryRepo;
    private final ProductRepository productRepo;
    private final LocationRepository locationRepo;

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    private static final int DEFAULT_REORDER_LEVEL = 10;

}