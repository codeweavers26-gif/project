package com.project.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductAttribute;
import com.project.backend.entity.ProductImage;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.LocationRepository;
import com.project.backend.repository.ProductAttributeRepository;
import com.project.backend.repository.ProductImageRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductRequestDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final ProductAttributeRepository productAttributeRepository ;
    private final ProductImageRepository productImageRepository;

    // CREATE PRODUCT
    @Transactional
    @CacheEvict(value = {"productsByLocation", "productDetails"}, allEntries = true)
    public ProductResponseDto create(ProductRequestDto dto) {

        if (dto.getImages() != null && dto.getImages().size() > 6) {
            throw new RuntimeException("Maximum 6 images allowed per product");
        }

        Product product = Product.builder()
                .name(dto.getName())
                .brand(dto.getBrand())
                .sku(dto.getSku())
                .slug(dto.getSlug())
                .shortDescription(dto.getShortDescription())
                .description(dto.getDescription())
                .mrp(dto.getMrp())
                .price(dto.getPrice())
                .discountPercent(dto.getDiscountPercent())
                .taxPercent(dto.getTaxPercent())
                .stock(dto.getStock())
                .weight(dto.getWeight())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .codAvailable(dto.getCodAvailable())
                .returnable(dto.getReturnable())
                .deliveryDays(dto.getDeliveryDays())
                .isActive(true)
                .build();

        productRepository.save(product);

        // 🖼️ Save Images
        if (dto.getImages() != null) {
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(dto.getImages().get(i))
                        .position(i + 1)
                        .build();
                productImageRepository.save(image);
            }
        }

        // 🏷️ Save Attributes
        if (dto.getAttributes() != null) {
            dto.getAttributes().forEach((key, value) -> {
                ProductAttribute attr = ProductAttribute.builder()
                        .product(product)
                        .name(key)
                        .value(value)
                        .build();
                productAttributeRepository.save(attr);
            });
        }

        return mapToResponse(product);
    }


    // UPDATE PRODUCT
    @Transactional
    @CacheEvict(value = {"productsByLocation", "productDetails"}, allEntries = true)
    public ProductResponseDto update(Long id, ProductRequestDto dto) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 🔹 Basic Info
        product.setName(dto.getName());
        product.setBrand(dto.getBrand());
        product.setSku(dto.getSku());
        product.setSlug(dto.getSlug());
        product.setShortDescription(dto.getShortDescription());
        product.setDescription(dto.getDescription());

        // 🔹 Pricing
        product.setMrp(dto.getMrp());
        product.setPrice(dto.getPrice());
        product.setDiscountPercent(dto.getDiscountPercent());
        product.setTaxPercent(dto.getTaxPercent());

        // 🔹 Inventory & Shipping
        product.setStock(dto.getStock());
        product.setWeight(dto.getWeight());
        product.setLength(dto.getLength());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setCodAvailable(dto.getCodAvailable());
        product.setReturnable(dto.getReturnable());
        product.setDeliveryDays(dto.getDeliveryDays());

        productRepository.save(product);

        // 🖼️ Replace Images
        if (dto.getImages() != null) {

            if (dto.getImages().size() > 6) {
                throw new RuntimeException("Maximum 6 images allowed per product");
            }

            productImageRepository.deleteByProduct(product);

            for (int i = 0; i < dto.getImages().size(); i++) {
                productImageRepository.save(ProductImage.builder()
                        .product(product)
                        .imageUrl(dto.getImages().get(i))
                        .position(i + 1)
                        .build());
            }
        }

        // 🏷️ Replace Attributes
        if (dto.getAttributes() != null) {
            productAttributeRepository.deleteByProduct(product);

            dto.getAttributes().forEach((key, value) ->
                    productAttributeRepository.save(
                            ProductAttribute.builder()
                                    .product(product)
                                    .name(key)
                                    .value(value)
                                    .build()
                    )
            );
        }

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
System.err.println("dcfv");
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductResponseDto mapToResponse(Product product) {

        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream()
                    .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
                    .map(ProductImage::getImageUrl)
                    .toList()
                : List.of();

        Map<String, String> attributes = product.getAttributes() != null
                ? product.getAttributes().stream()
                    .collect(Collectors.toMap(
                            ProductAttribute::getName,
                            ProductAttribute::getValue
                    ))
                : Map.of();

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .mrp(product.getMrp())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .taxPercent(product.getTaxPercent())
                .stock(product.getStock())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .codAvailable(product.getCodAvailable())
                .returnable(product.getReturnable())
                .deliveryDays(product.getDeliveryDays())
                .images(imageUrls)
                .attributes(attributes)
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
