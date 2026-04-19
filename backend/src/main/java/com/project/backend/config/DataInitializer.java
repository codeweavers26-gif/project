package com.project.backend.config;

import com.project.backend.entity.Warehouse;
import com.project.backend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initDefaultWarehouse();
    }

    private void initDefaultWarehouse() {
        boolean exists = warehouseRepository.findDefaultWarehouse().isPresent();
        if (!exists) {
            Warehouse warehouse = Warehouse.builder()
                    .name("Main Warehouse")
                    .code("MAIN-WH")
                    .isDefault(true)
                    .isActive(true)
                    .build();
            warehouseRepository.save(warehouse);
            log.info("Default warehouse created automatically.");
        } else {
            log.info("Default warehouse already exists. Skipping.");
        }
    }
}
