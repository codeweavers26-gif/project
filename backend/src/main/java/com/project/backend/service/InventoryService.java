package com.project.backend.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import com.project.backend.entity.Location;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.ProductInventoryRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.InventoryRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final ProductRepository productRepository;
	private final LocationRepository locationRepository;
	private final ProductInventoryRepository inventoryRepository;

	@CacheEvict(value = "productsByLocation", allEntries = true)
	public void upsertInventory(InventoryRequestDto dto) {

		Product product = productRepository.findById(dto.getProductId())
				.orElseThrow(() -> new NotFoundException("Product not found"));

		Location location = locationRepository.findById(dto.getLocationId())
				.orElseThrow(() -> new NotFoundException("Location not found"));

		ProductInventory inventory = inventoryRepository.findByProductAndLocation(product, location)
				.orElse(ProductInventory.builder().product(product).location(location).stock(0).build());

		inventory.setStock(dto.getStock());
		inventoryRepository.save(inventory);
	}

	public List<ProductInventory> getInventoryByProduct(Long productId) {

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Product not found"));

		return inventoryRepository.findByProduct(product);
	}

	public List<ProductInventory> getInventoryByLocation(Long locationId) {

		Location location = locationRepository.findById(locationId)
				.orElseThrow(() -> new NotFoundException("Location not found"));

		return inventoryRepository.findByLocation(location);
	}

	public List<ProductInventory> getLowStock(int threshold) {
		return inventoryRepository.findByStockLessThan(threshold);
	}

	@CacheEvict(value = "productsByLocation", allEntries = true)
	public void bulkUpsert(List<InventoryRequestDto> dtos) {
		dtos.forEach(this::upsertInventory);
	}

}
