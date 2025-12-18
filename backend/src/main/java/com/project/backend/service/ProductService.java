package com.project.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.entity.Product;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    // CREATE PRODUCT
    @CacheEvict(value = {"productsByLocation", "productDetails"}, allEntries = true)
    public ProductResponseDto create(ProductRequestDto dto) {

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrl(dto.getImageUrl())
                .isActive(true)
                .build();

        productRepository.save(product);

        return mapToResponse(product);
    }

    // UPDATE PRODUCT
    @CacheEvict(value = {"productsByLocation", "productDetails"}, allEntries = true)
    public ProductResponseDto update(Long id, ProductRequestDto dto) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setImageUrl(dto.getImageUrl());

        productRepository.save(product);

        return mapToResponse(product);
    }

    // SOFT DELETE
    public void deactivate(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setIsActive(false);
        productRepository.save(product);
    }

    // ADMIN VIEW ALL
    public List<ProductResponseDto> getAll() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductResponseDto mapToResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .build();
    }
 // CUSTOMER VIEW - ALL ACTIVE PRODUCTS
    public PageResponseDto<ProductResponseDto> getActiveProducts(
            int page, int size, String sortBy) {

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy).descending()
        );

        Page<Product> productPage =
                productRepository.findByIsActiveTrue(pageable);

        return PageResponseDto.<ProductResponseDto>builder()
                .content(productPage.getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    // CUSTOMER VIEW - PRODUCT BY ID
    @Cacheable(value = "productDetails", key = "#productId")
    public ProductResponseDto getActiveProductById(Long id) {

        Product product = productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return mapToResponse(product);
    }
    @Cacheable(
    	    value = "productsByLocation",
    	    key = "#locationId + '_' + #page + '_' + #size"
    	)
    public PageResponseDto<ProductResponseDto> getProductsByLocation(
            Long locationId, int page, int size) {
    	 if (!locationRepository.existsById(locationId)) {
             throw new NotFoundException("Location not found");
         }
        Page<Product> products =
                productRepository.findAvailableProducts(locationId,
                        PageRequest.of(page, size));

        return mapToPageResponse(products);
    }
    private PageResponseDto<ProductResponseDto> mapToPageResponse(
            Page<Product> productPage) {

        return PageResponseDto.<ProductResponseDto>builder()
                .content(
                        productPage.getContent()
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }


}
