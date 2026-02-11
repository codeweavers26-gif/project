package com.project.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.InventoryResponseDto;
import com.project.backend.entity.Location;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.ProductInventoryRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.InventoryRequestDto;
import com.project.backend.requestDto.PageResponseDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductInventoryRepository inventoryRepo;
    private final ProductRepository productRepo;
    private final LocationRepository locationRepo;

    /* =================================================
       ADMIN – CREATE / UPDATE INVENTORY
       ================================================= */
    @Transactional
    public void upsertInventory(InventoryRequestDto dto) {

        Product product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Location location = locationRepo.findById(dto.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseGet(() -> ProductInventory.builder()
                        .product(product)
                        .location(location)
                        .stock(0)
                        .build());
        inventory.setProduct(product);
        inventory.setLocation(location);
        inventory.setStock(dto.getStock());
        inventoryRepo.save(inventory);
    }

    /* =================================================
       ADMIN – SEARCH INVENTORY (PAGINATED)
       ================================================= */
    public PageResponseDto<InventoryResponseDto> searchInventory(
            Long productId,
            Long locationId,
            int page,
            int size) {

        Page<ProductInventory> pageData =
                inventoryRepo.search(
                        productId,
                        locationId,
                        PageRequest.of(page, size, Sort.by("id").descending())
                );

        return mapToPage(pageData);
    }

    /* =================================================
       ADMIN – MANUAL ADJUST (+ / -)
       ================================================= */
    @Transactional
    public InventoryResponseDto adjustStock(Long inventoryId, int delta) {

        ProductInventory inventory = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        int newStock = inventory.getStock() + delta;

        if (newStock < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        inventory.setStock(newStock);
        inventoryRepo.save(inventory);

        return mapToDto(inventory);
    }

    /* =================================================
       SYSTEM – ORDER PLACED (DEDUCT)
       ================================================= */
    @Transactional
    public void deductStock(Product product, Location location, int qty) {

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseThrow(() -> new BadRequestException("Inventory not found"));

        if (inventory.getStock() < qty) {
            throw new BadRequestException("Insufficient stock");
        }

        inventory.setStock(inventory.getStock() - qty);
        inventoryRepo.save(inventory);
    }

    /* =================================================
       SYSTEM – RETURN / CANCEL (RESTORE)
       ================================================= */
    @Transactional
    public void restoreStock(Product product, Location location, int qty) {

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseThrow(() -> new BadRequestException("Inventory not found"));

        inventory.setStock(inventory.getStock() + qty);
        inventoryRepo.save(inventory);
    }

    /* =================================================
       ADMIN – LOW STOCK
       ================================================= */
    public PageResponseDto<InventoryResponseDto> getLowStock(
            int threshold,
            int page,
            int size) {

        Page<ProductInventory> pageData =
                inventoryRepo.findByStockLessThan(
                        threshold,
                        PageRequest.of(page, size)
                );

        return mapToPage(pageData);
    }

    /* =================================================
       ADMIN – OUT OF STOCK
       ================================================= */
    public List<InventoryResponseDto> getOutOfStock() {

        return inventoryRepo.findByStock(0)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /* =================================================
       ADMIN – BULK UPSERT
       ================================================= */
    @Transactional
    public void bulkUpsert(List<InventoryRequestDto> dtos) {
        dtos.forEach(this::upsertInventory);
    }

    /* =================================================
       MAPPERS
       ================================================= */
    private PageResponseDto<InventoryResponseDto> mapToPage(Page<ProductInventory> page) {

        return PageResponseDto.<InventoryResponseDto>builder()
                .content(page.getContent().stream().map(this::mapToDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private InventoryResponseDto mapToDto(ProductInventory i) {

        return InventoryResponseDto.builder()
                .inventoryId(i.getId())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .locationId(i.getLocation().getId())
                .locationName(i.getLocation().getName())
                .stock(i.getStock())
                .build();
    }
}
