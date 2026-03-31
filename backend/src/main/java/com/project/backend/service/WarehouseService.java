package com.project.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.OrderItem;
import com.project.backend.entity.Return;
import com.project.backend.entity.ReturnItem;
import com.project.backend.entity.Warehouse;
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderItemRepository;
import com.project.backend.repository.WarehouseInventoryRepository;
import com.project.backend.repository.WarehouseRepository;
import com.project.backend.requestDto.WarehouseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

	private final WarehouseRepository warehouseRepository;
	private final WarehouseInventoryRepository inventoryRepository;
	private final OrderItemRepository orderItemRepository; 

private static final int LOW_STOCK_THRESHOLD = 5;
private static final int DEFAULT_NEAREST_WAREHOUSE_LIMIT = 5;
private static final String CACHE_WAREHOUSES = "warehouses";
private static final String CACHE_DEFAULT_WAREHOUSE = "defaultWarehouse";
private static final String CACHE_INVENTORY_SUMMARY = "warehouseInventorySummary";
	@Transactional
@CacheEvict(value = {CACHE_WAREHOUSES, CACHE_DEFAULT_WAREHOUSE}, allEntries = true)
public WarehouseDto.Response createWarehouse(WarehouseDto.Request request) {
    
    validateWarehouseRequest(request);
    
    if (Boolean.TRUE.equals(request.getIsDefault())) {
        clearExistingDefault();
    }

    Warehouse warehouse = buildWarehouseFromRequest(request);
    Warehouse savedWarehouse = warehouseRepository.save(warehouse);
    
    log.info("Warehouse created: {} - {}", savedWarehouse.getCode(), savedWarehouse.getName());
    return mapToResponse(savedWarehouse);
}

private void validateWarehouseRequest(WarehouseDto.Request request) {
    if (request.getName() == null || request.getName().trim().isEmpty()) {
        throw new BadRequestException("Warehouse name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
        throw new BadRequestException("Warehouse code is required");
    }
    if (request.getPincode() == null || request.getPincode().trim().isEmpty()) {
        throw new BadRequestException("Pincode is required");
    }
    
    if (warehouseRepository.existsByCode(request.getCode())) {
        throw new BadRequestException("Warehouse code already exists: " + request.getCode());
    }
}
private Warehouse buildWarehouseFromRequest(WarehouseDto.Request request) {
    return Warehouse.builder()
            .name(request.getName())
            .code(request.getCode())
            .locationId(request.getLocationId())
            .address(request.getAddress())
            .city(request.getCity())
            .state(request.getState())
            .pincode(request.getPincode())
            .contactPerson(request.getContactPerson())
            .contactPhone(request.getContactPhone())
            .contactEmail(request.getContactEmail())
            .isDefault(request.getIsDefault())
            .isActive(request.getIsActive())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .build();
}

 private Warehouse getWarehouseByIdOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));
    }


	@Transactional
@CacheEvict(value = {CACHE_WAREHOUSES, CACHE_DEFAULT_WAREHOUSE}, allEntries = true)
public WarehouseDto.Response updateWarehouse(Long id, WarehouseDto.Request request) {

    Warehouse warehouse = getWarehouseByIdOrThrow(id);
    
    validateWarehouseUpdate(request, warehouse);
    
    if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(warehouse.getIsDefault())) {
        clearExistingDefault();
    }

    updateWarehouseFromRequest(warehouse, request);
    
    Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
    log.info("Warehouse updated: {} - {}", updatedWarehouse.getCode(), updatedWarehouse.getName());

    return mapToResponse(updatedWarehouse);
}

private void validateWarehouseUpdate(WarehouseDto.Request request, Warehouse existingWarehouse) {
    if (request.getCode() != null && !request.getCode().equals(existingWarehouse.getCode()) &&
        warehouseRepository.existsByCode(request.getCode())) {
        throw new BadRequestException("Warehouse code already exists: " + request.getCode());
    }
}

private void updateWarehouseFromRequest(Warehouse warehouse, WarehouseDto.Request request) {
    Optional.ofNullable(request.getName()).ifPresent(warehouse::setName);
    Optional.ofNullable(request.getCode()).ifPresent(warehouse::setCode);
    Optional.ofNullable(request.getLocationId()).ifPresent(warehouse::setLocationId);
    Optional.ofNullable(request.getAddress()).ifPresent(warehouse::setAddress);
    Optional.ofNullable(request.getCity()).ifPresent(warehouse::setCity);
    Optional.ofNullable(request.getState()).ifPresent(warehouse::setState);
    Optional.ofNullable(request.getPincode()).ifPresent(warehouse::setPincode);
    Optional.ofNullable(request.getContactPerson()).ifPresent(warehouse::setContactPerson);
    Optional.ofNullable(request.getContactPhone()).ifPresent(warehouse::setContactPhone);
    Optional.ofNullable(request.getContactEmail()).ifPresent(warehouse::setContactEmail);
    Optional.ofNullable(request.getIsDefault()).ifPresent(warehouse::setIsDefault);
    Optional.ofNullable(request.getIsActive()).ifPresent(warehouse::setIsActive);
    Optional.ofNullable(request.getLatitude()).ifPresent(warehouse::setLatitude);
    Optional.ofNullable(request.getLongitude()).ifPresent(warehouse::setLongitude);
}

	@Transactional
@CacheEvict(value = {CACHE_WAREHOUSES, CACHE_DEFAULT_WAREHOUSE}, allEntries = true)
public void deleteWarehouse(Long id) {
    if (!warehouseRepository.existsById(id)) {
        throw new NotFoundException("Warehouse not found with id: " + id);
    }

    if (inventoryRepository.countByWarehouseId(id) > 0) {
        throw new BadRequestException("Cannot delete warehouse with existing inventory. Deactivate it instead.");
    }

    warehouseRepository.deleteById(id);
    log.info("Warehouse deleted: {}", id);
}

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public void deactivateWarehouse(Long id) {
		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

		warehouse.setIsActive(false);

		if (Boolean.TRUE.equals(warehouse.getIsDefault())) {
			warehouse.setIsDefault(false);
		}

		warehouseRepository.save(warehouse);
		log.info("Warehouse deactivated: {}", id);
	}

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public void setDefaultWarehouse(Long id) {
		clearExistingDefault();

		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

		warehouse.setIsDefault(true);
		warehouseRepository.save(warehouse);
		log.info("Default warehouse set: {}", id);
	}


	@Cacheable(value = "defaultWarehouse")
	public WarehouseDto.Response getDefaultWarehouse() {
		Warehouse warehouse = warehouseRepository.findDefaultWarehouse()
				.orElseThrow(() -> new NotFoundException("No default warehouse configured"));
		return mapToResponse(warehouse);
	}

	@Cacheable(value = "warehouses", key = "#id")
	public WarehouseDto.Response getWarehouseById(Long id) {
		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));
		return mapToResponse(warehouse);
	}

	@Cacheable(value = "warehouses", key = "'location_' + #locationId")
	public WarehouseDto.Response getWarehouseByLocation(Long locationId) {
		Warehouse warehouse = warehouseRepository.findByLocationId(locationId)
				.orElseThrow(() -> new NotFoundException("Warehouse not found for location: " + locationId));
		return mapToResponse(warehouse);
	}

	public List<WarehouseDto.Response> getAllActiveWarehouses() {
		return warehouseRepository.findByIsActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	public Page<WarehouseDto.Response> getActiveWarehousesPaginated(Pageable pageable) {
		return warehouseRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
	}

	@Cacheable(value = "warehouseInventorySummary")
	public WarehouseDto.InventorySummary getWarehouseInventorySummary(Long warehouseId) {
		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + warehouseId));

		List<WarehouseInventory> inventories = warehouse.getInventories();

		int totalVariants = inventories.size();
		int totalStock = inventories.stream().mapToInt(WarehouseInventory::getAvailableQuantity).sum();
		int lowStockCount = (int) inventories.stream()
				.filter(i -> i.getAvailableQuantity() > 0 && i.getAvailableQuantity() < 5).count();
		int outOfStockCount = (int) inventories.stream().filter(i -> i.getAvailableQuantity() == 0).count();

		return WarehouseDto.InventorySummary.builder().warehouseId(warehouseId).warehouseName(warehouse.getName())
				.totalVariants(totalVariants).totalStock(totalStock).lowStockCount(lowStockCount)
				.outOfStockCount(outOfStockCount).build();
	}


	@Transactional(readOnly = true)
	public Warehouse selectWarehouseForOrder(String customerState, String customerCity, Long variantId) {

		if (isDelhiOnlyMode()) {
			return warehouseRepository.findDefaultWarehouse()
					.orElseThrow(() -> new NotFoundException("No default warehouse available"));
		}

		List<Warehouse> nearestWarehouses = warehouseRepository.findNearestWarehouses(customerState, customerCity,
				PageRequest.of(0, 5));

		for (Warehouse warehouse : nearestWarehouses) {
			if (warehouseRepository.hasVariantInStock(warehouse.getId(), variantId)) {
				return warehouse;
			}
		}

		throw new NotFoundException("No warehouse with stock found for variant: " + variantId);
	}

	private void clearExistingDefault() {
		warehouseRepository.findDefaultWarehouse().ifPresent(existingDefault -> {
			existingDefault.setIsDefault(false);
			warehouseRepository.save(existingDefault);
		});
	}

	private boolean isDelhiOnlyMode() {
		return true;
	}

	private WarehouseDto.Response mapToResponse(Warehouse warehouse) {
		Integer totalVariants = warehouse.getInventories() != null ? warehouse.getInventories().size() : 0;
		Integer totalStock = warehouse.getInventories() != null
				? warehouse.getInventories().stream().mapToInt(WarehouseInventory::getAvailableQuantity).sum()
				: 0;

		return WarehouseDto.Response.builder().id(warehouse.getId()).name(warehouse.getName()).code(warehouse.getCode())
				.locationId(warehouse.getLocationId()).address(warehouse.getAddress()).city(warehouse.getCity())
				.state(warehouse.getState()).pincode(warehouse.getPincode()).contactPerson(warehouse.getContactPerson())
				.contactPhone(warehouse.getContactPhone()).contactEmail(warehouse.getContactEmail())
				.isDefault(warehouse.getIsDefault()).isActive(warehouse.getIsActive()).latitude(warehouse.getLatitude())
				.longitude(warehouse.getLongitude()).createdAt(warehouse.getCreatedAt())
				.updatedAt(warehouse.getUpdatedAt()).totalVariants(totalVariants).totalStock(totalStock).build();
	}


	@Transactional
public void restock(Return ret) {

    if (Boolean.TRUE.equals(ret.getInventoryRestocked())) {
        log.info("Inventory already restocked for returnId={}", ret.getId());
        return;
    }

    Long warehouseId = ret.getOrder().getWarehouse().getId();

    for (ReturnItem ri : ret.getItems()) {

        OrderItem oi = orderItemRepository.findById(ri.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        Long variantId = oi.getVariantId();
        int qty = ri.getQuantity();

        WarehouseInventory inventory =
                inventoryRepository
                        .findByWarehouseIdAndVariantId(warehouseId, variantId)
                        .orElseThrow(() ->
                                new RuntimeException("Inventory not found for variant=" + variantId)
                        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + qty
        );

        inventoryRepository.save(inventory);

        log.info("Restocked variant={}, qty={}, warehouse={}",
                variantId, qty, warehouseId);
    }

    ret.setInventoryRestocked(true);
}
}