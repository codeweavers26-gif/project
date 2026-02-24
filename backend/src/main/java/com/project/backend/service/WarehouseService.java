package com.project.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.Warehouse;
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
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

	// ============= ADMIN APIs =============

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public WarehouseDto.Response createWarehouse(WarehouseDto.Request request) {

		// Check if only one default warehouse
		if (Boolean.TRUE.equals(request.getIsDefault())) {
			clearExistingDefault();
		}

		Warehouse warehouse = Warehouse.builder().name(request.getName()).code(request.getCode())
				.locationId(request.getLocationId()).address(request.getAddress()).city(request.getCity())
				.state(request.getState()).pincode(request.getPincode()).contactPerson(request.getContactPerson())
				.contactPhone(request.getContactPhone()).contactEmail(request.getContactEmail())
				.isDefault(request.getIsDefault()).isActive(request.getIsActive()).latitude(request.getLatitude())
				.longitude(request.getLongitude()).build();

		Warehouse savedWarehouse = warehouseRepository.save(warehouse);
		log.info("Warehouse created: {} - {}", savedWarehouse.getCode(), savedWarehouse.getName());

		return mapToResponse(savedWarehouse);
	}

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public WarehouseDto.Response updateWarehouse(Long id, WarehouseDto.Request request) {

		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

		// Handle default flag change
		if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(warehouse.getIsDefault())) {
			clearExistingDefault();
		}

		warehouse.setName(request.getName());
		warehouse.setCode(request.getCode());
		warehouse.setLocationId(request.getLocationId());
		warehouse.setAddress(request.getAddress());
		warehouse.setCity(request.getCity());
		warehouse.setState(request.getState());
		warehouse.setPincode(request.getPincode());
		warehouse.setContactPerson(request.getContactPerson());
		warehouse.setContactPhone(request.getContactPhone());
		warehouse.setContactEmail(request.getContactEmail());
		warehouse.setIsDefault(request.getIsDefault());
		warehouse.setIsActive(request.getIsActive());
		warehouse.setLatitude(request.getLatitude());
		warehouse.setLongitude(request.getLongitude());

		Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
		log.info("Warehouse updated: {} - {}", updatedWarehouse.getCode(), updatedWarehouse.getName());

		return mapToResponse(updatedWarehouse);
	}

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public void deleteWarehouse(Long id) {
		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

		// Check if warehouse has inventory
		if (!warehouse.getInventories().isEmpty()) {
			throw new BadRequestException("Cannot delete warehouse with existing inventory. Deactivate it instead.");
		}

		warehouseRepository.delete(warehouse);
		log.info("Warehouse deleted: {}", id);
	}

	@Transactional
	@CacheEvict(value = { "warehouses", "defaultWarehouse" }, allEntries = true)
	public void deactivateWarehouse(Long id) {
		Warehouse warehouse = warehouseRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Warehouse not found with id: " + id));

		warehouse.setIsActive(false);

		// If this was default, clear default flag
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

	// ============= PUBLIC APIs =============

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

	// ============= WAREHOUSE SELECTION LOGIC =============

	@Transactional(readOnly = true)
	public Warehouse selectWarehouseForOrder(String customerState, String customerCity, Long variantId) {

		// For Delhi launch: just return default warehouse
		if (isDelhiOnlyMode()) {
			return warehouseRepository.findDefaultWarehouse()
					.orElseThrow(() -> new NotFoundException("No default warehouse available"));
		}

		// For multi-warehouse: find nearest with stock
		List<Warehouse> nearestWarehouses = warehouseRepository.findNearestWarehouses(customerState, customerCity,
				PageRequest.of(0, 5));

		for (Warehouse warehouse : nearestWarehouses) {
			if (warehouseRepository.hasVariantInStock(warehouse.getId(), variantId)) {
				return warehouse;
			}
		}

		throw new NotFoundException("No warehouse with stock found for variant: " + variantId);
	}

	// ============= PRIVATE HELPER METHODS =============

	private void clearExistingDefault() {
		warehouseRepository.findDefaultWarehouse().ifPresent(existingDefault -> {
			existingDefault.setIsDefault(false);
			warehouseRepository.save(existingDefault);
		});
	}

	private boolean isDelhiOnlyMode() {
		// For launch: return true
		// Later: make this configurable
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
}