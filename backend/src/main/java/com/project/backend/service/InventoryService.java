package com.project.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.InventoryDashboardDto;
import com.project.backend.ResponseDto.InventoryResponseDto;
import com.project.backend.ResponseDto.LowStockItemDto;
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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductInventoryRepository inventoryRepo;
    private final ProductRepository productRepo;
    private final LocationRepository locationRepo;

    // Default thresholds
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    private static final int DEFAULT_REORDER_LEVEL = 10;

    /* =================================================
       ADMIN – CREATE / UPDATE INVENTORY
       ================================================= */
    @Transactional
    public void upsertInventory(InventoryRequestDto dto) {

        Product product = productRepo.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + dto.getProductId()));

        Location location = locationRepo.findById(dto.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found: " + dto.getLocationId()));

        if (dto.getStock() < 0) {
            throw new BadRequestException("Stock cannot be negative");
        }

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseGet(() -> ProductInventory.builder()
                        .product(product)
                        .location(location)
                        .stock(0)
                        .build());

        // ✅ Log stock changes for audit
        int oldStock = inventory.getStock();
        inventory.setStock(dto.getStock());
        inventoryRepo.save(inventory);

        log.info("Inventory updated: Product={}, Location={}, Old Stock={}, New Stock={}, Changed by={}", 
                product.getId(), location.getId(), oldStock, dto.getStock(), "ADMIN");
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
                        PageRequest.of(page, size, Sort.by("product.name").ascending())
                );

        return mapToPage(pageData);
    }

    /* =================================================
       ADMIN – MANUAL ADJUST (+ / -)
       ================================================= */
    @Transactional
    public InventoryResponseDto adjustStock(Long inventoryId, int delta) {

        ProductInventory inventory = inventoryRepo.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with ID: " + inventoryId));

        int newStock = inventory.getStock() + delta;

        if (newStock < 0) {
            throw new BadRequestException(
                String.format("Cannot reduce stock below 0. Current: %d, Requested reduction: %d", 
                inventory.getStock(), Math.abs(delta)));
        }

        int oldStock = inventory.getStock();
        inventory.setStock(newStock);
        inventoryRepo.save(inventory);

        log.info("Stock adjusted: Inventory={}, Old={}, New={}, Delta={}, Reason={}", 
                inventoryId, oldStock, newStock, delta, "ADMIN_ADJUSTMENT");

        return mapToDto(inventory);
    }

    /* =================================================
       SYSTEM – ORDER PLACED (DEDUCT)
       ================================================= */
    @Transactional
    public void deductStock(Product product, Location location, int qty) {

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseThrow(() -> new BadRequestException(
                    "Inventory not found for Product: " + product.getId() + " at Location: " + location.getId()));

        if (inventory.getStock() < qty) {
            throw new BadRequestException(
                String.format("Insufficient stock for %s. Available: %d, Requested: %d", 
                product.getName(), inventory.getStock(), qty));
        }

        inventory.setStock(inventory.getStock() - qty);
        inventoryRepo.save(inventory);

        log.info("Stock deducted: Product={}, Location={}, Qty={}, Remaining={}, Reason={}", 
                product.getId(), location.getId(), qty, inventory.getStock(), "ORDER_PLACED");
    }

    /* =================================================
       SYSTEM – RETURN / CANCEL (RESTORE)
       ================================================= */
    @Transactional
    public void restoreStock(Product product, Location location, int qty) {

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseThrow(() -> new BadRequestException(
                    "Inventory not found for Product: " + product.getId() + " at Location: " + location.getId()));

        inventory.setStock(inventory.getStock() + qty);
        inventoryRepo.save(inventory);

        log.info("Stock restored: Product={}, Location={}, Qty={}, New Stock={}, Reason={}", 
                product.getId(), location.getId(), qty, inventory.getStock(), "ORDER_CANCELLED");
    }

    /* =================================================
       ADMIN – LOW STOCK
       ================================================= */
    public PageResponseDto<InventoryResponseDto> getLowStock(
            int threshold,
            int page,
            int size) {

        if (threshold <= 0) {
            threshold = DEFAULT_LOW_STOCK_THRESHOLD;
        }

        Page<ProductInventory> pageData =
                inventoryRepo.findByStockLessThan(
                        threshold,
                        PageRequest.of(page, size, Sort.by("stock").ascending())
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
                .collect(Collectors.toList());
    }

    /* =================================================
       ADMIN – BULK UPSERT
       ================================================= */
    @Transactional
    public void bulkUpsert(List<InventoryRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("Bulk inventory list cannot be empty");
        }

        log.info("Starting bulk upsert of {} inventory items", dtos.size());
        dtos.forEach(this::upsertInventory);
        log.info("Bulk upsert completed successfully");
    }

    /* =================================================
       NEW: GET INVENTORY DASHBOARD STATS
       ================================================= */
    public InventoryDashboardDto getInventoryDashboardStats() {

        Long totalProducts = productRepo.count();
        Long totalLocations = locationRepo.count();
        Long outOfStockCount = inventoryRepo.countOutOfStock();
        Double avgStock = inventoryRepo.getAverageStock();
        Double totalValue = inventoryRepo.getTotalInventoryValue();
        List<Object[]> lowStockReport = inventoryRepo.getLowStockReport(DEFAULT_LOW_STOCK_THRESHOLD);

        return InventoryDashboardDto.builder()
                .totalProducts(totalProducts)
                .totalLocations(totalLocations)
                .totalInventoryItems(inventoryRepo.count())
                .outOfStockCount(outOfStockCount)
                .inStockCount(totalProducts * totalLocations - outOfStockCount) // approximate
                .averageStockPerProduct(avgStock != null ? avgStock : 0.0)
                .totalInventoryValue(totalValue != null ? totalValue : 0.0)
                .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                .lowStockCount((long) lowStockReport.size())
                .lowStockItems(lowStockReport.stream()
                        .map(row -> new LowStockItemDto((String) row[0], (Integer) row[1]))
                        .collect(Collectors.toList()))
                .build();
    }

    /* =================================================
       NEW: GET INVENTORY BY PRODUCT
       ================================================= */
    public List<InventoryResponseDto> getInventoryByProduct(Long productId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        return inventoryRepo.search(productId, null, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /* =================================================
       NEW: GET INVENTORY BY LOCATION
       ================================================= */
    public List<InventoryResponseDto> getInventoryByLocation(Long locationId) {

        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found: " + locationId));

        return inventoryRepo.search(null, locationId, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /* =================================================
       NEW: VALIDATE STOCK BEFORE CHECKOUT
       ================================================= */
    public void validateStock(Product product, Location location, int requestedQty) {

        ProductInventory inventory = inventoryRepo
                .findByProductAndLocation(product, location)
                .orElseThrow(() -> new BadRequestException(
                    "Product not available at selected location"));

        if (inventory.getStock() < requestedQty) {
            throw new BadRequestException(
                String.format("Only %d units of %s available at your location", 
                inventory.getStock(), product.getName()));
        }
    }

    /* =================================================
       MAPPERS (ENHANCED)
       ================================================= */
    private PageResponseDto<InventoryResponseDto> mapToPage(Page<ProductInventory> page) {

        return PageResponseDto.<InventoryResponseDto>builder()
                .content(page.getContent().stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private InventoryResponseDto mapToDto(ProductInventory i) {

        int stock = i.getStock() != null ? i.getStock() : 0;
        String stockStatus;
        
        if (stock <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (stock <= DEFAULT_LOW_STOCK_THRESHOLD) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }

        return InventoryResponseDto.builder()
                .inventoryId(i.getId())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .locationId(i.getLocation().getId())
                .locationName(i.getLocation().getName())
                .stock(stock)
                .lastUpdated(i.getCreatedAt() != null ? i.getCreatedAt() : Instant.now())
                .availableStock(stock)  // Simple version
                .build();
    }
}