package com.project.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.entity.Category;
import com.project.backend.entity.Product;
import com.project.backend.entity.Section;
import com.project.backend.entity.SubCategory;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.ProductAttributeRepository; // Add this
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.repository.SubCategoryRepository;
import com.project.backend.requestDto.AttributeFilterDto;
import com.project.backend.requestDto.BreadcrumbDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductFilterDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductFilterService {

    private final ProductRepository productRepo;
    private final SectionRepository sectionRepo;
    private final CategoryRepository categoryRepo;
    private final SubCategoryRepository subCategoryRepo;
    private final ProductAttributeRepository attributeRepo; // Add this dependency

    /**
     * Filter products with pagination
     */
    public PageResponseDto<ProductResponseDto> filterProducts(ProductFilterDto filter) {
        
        Pageable pageable = createPageable(filter);
        
        // Handle multiple brands - convert list to comma-separated or use IN clause
        String brand = (filter.getBrands() != null && !filter.getBrands().isEmpty()) 
                ? filter.getBrands().get(0) // Simplified - for multiple brands, you need custom query
                : null;
        
        Page<Product> products = productRepo.filterProducts(
                filter.getSectionId(),
                filter.getCategoryId(),
                filter.getSubCategoryId(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                brand, // Single brand for now
                filter.getInStockOnly(),
                filter.getMinRating(),
                filter.getCodAvailable(),
                pageable
        );
        
        return mapToPageResponse(products);
    }
    
    /**
     * Get filter options for current hierarchy level
     */
    public Map<String, Object> getFilterOptions(Long sectionId, Long categoryId, Long subCategoryId) {
        
        Map<String, Object> options = new HashMap<>();
        
        // Get current level info
        String currentLevel = getCurrentLevelName(sectionId, categoryId, subCategoryId);
        options.put("currentLevel", currentLevel);
        
        // Get breadcrumb
        options.put("breadcrumb", getBreadcrumb(sectionId, categoryId, subCategoryId));
        
        // Get available brands
        List<String> brands = productRepo.findDistinctBrandsByHierarchy(
                sectionId, categoryId, subCategoryId);
        options.put("brands", brands != null ? brands : new ArrayList<>());
        
        // Get price range
        Double minPrice = productRepo.findMinPriceByHierarchy(
                sectionId, categoryId, subCategoryId);
        Double maxPrice = productRepo.findMaxPriceByHierarchy(
                sectionId, categoryId, subCategoryId);
        
        Map<String, Double> priceRange = new HashMap<>();
        priceRange.put("min", minPrice != null ? minPrice : 0);
        priceRange.put("max", maxPrice != null ? maxPrice : 100000);
        options.put("priceRange", priceRange);
        
        // Get attribute options (sizes, colors, etc.)
        List<AttributeFilterDto> attributes = getAttributeOptions(sectionId, categoryId, subCategoryId);
        options.put("attributes", attributes);
        
        // Add counts
        Long totalProducts = productRepo.countByHierarchy(sectionId, categoryId, subCategoryId);
        options.put("totalProducts", totalProducts);
        
        return options;
    }
    
    /**
     * Get attribute options from ProductAttribute table
     */
    private List<AttributeFilterDto> getAttributeOptions(Long sectionId, Long categoryId, Long subCategoryId) {
        
        // You need to implement this based on your attribute structure
        // This is a placeholder - implement according to your attribute system
        List<AttributeFilterDto> attributes = new ArrayList<>();
        
        // Example for sizes
        List<String> sizes = productRepo.findDistinctSizesByHierarchy(
                sectionId, categoryId, subCategoryId);
        if (sizes != null && !sizes.isEmpty()) {
            attributes.add(AttributeFilterDto.builder()
                    .name("size")
                    .displayName("Size")
                    .options(sizes)
                    .build());
        }
        
        // Example for colors
        List<String> colors = productRepo.findDistinctColorsByHierarchy(
                sectionId, categoryId, subCategoryId);
        if (colors != null && !colors.isEmpty()) {
            attributes.add(AttributeFilterDto.builder()
                    .name("color")
                    .displayName("Color")
                    .options(colors)
                    .build());
        }
        
        return attributes;
    }
    
    /**
     * Get current level name
     */
    private String getCurrentLevelName(Long sectionId, Long categoryId, Long subCategoryId) {
        if (subCategoryId != null) {
            return subCategoryRepo.findById(subCategoryId)
                    .map(SubCategory::getName)
                    .orElse("");
        } else if (categoryId != null) {
            return categoryRepo.findById(categoryId)
                    .map(Category::getName)
                    .orElse("");
        } else if (sectionId != null) {
            return sectionRepo.findById(sectionId)
                    .map(Section::getName)
                    .orElse("");
        }
        return "All Products";
    }
    
    /**
     * Get breadcrumb navigation
     */
    public List<BreadcrumbDto> getBreadcrumb(Long sectionId, Long categoryId, Long subCategoryId) {
        
        List<BreadcrumbDto> breadcrumb = new ArrayList<>();
        
        if (sectionId != null) {
            sectionRepo.findById(sectionId).ifPresent(section -> 
                breadcrumb.add(new BreadcrumbDto("section", section.getName(), "/section/" + sectionId)));
        }
        
        if (categoryId != null) {
            categoryRepo.findById(categoryId).ifPresent(category -> 
                breadcrumb.add(new BreadcrumbDto("category", category.getName(), "/category/" + categoryId)));
        }
        
        if (subCategoryId != null) {
            subCategoryRepo.findById(subCategoryId).ifPresent(subCategory -> 
                breadcrumb.add(new BreadcrumbDto("subcategory", subCategory.getName(), "/subcategory/" + subCategoryId)));
        }
        
        return breadcrumb;
    }
    
    /**
     * Create Pageable based on sort options
     */
    private Pageable createPageable(ProductFilterDto filter) {
        Sort sort = Sort.by("createdAt").descending(); // default newest first
        
        if (filter.getSortBy() != null) {
            switch (filter.getSortBy()) {
                case "price_asc":
                    sort = Sort.by("price").ascending();
                    break;
                case "price_desc":
                    sort = Sort.by("price").descending();
                    break;
                case "rating_desc":
                    sort = Sort.by("averageRating").descending();
                    break;
                case "popularity":
                    sort = Sort.by("totalReviews").descending();
                    break;
                case "name_asc":
                    sort = Sort.by("name").ascending();
                    break;
            }
        }
        
        return PageRequest.of(filter.getPage(), filter.getSize(), sort);
    }
    
    /**
     * Map Page<Product> to PageResponseDto<ProductResponseDto>
     */
    private PageResponseDto<ProductResponseDto> mapToPageResponse(Page<Product> page) {
        
        List<ProductResponseDto> content = page.getContent().stream()
                .map(this::mapToProductDto)
                .collect(Collectors.toList());
        
        return PageResponseDto.<ProductResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    
    /**
     * Map Product to ProductResponseDto
     */
    private ProductResponseDto mapToProductDto(Product product) {
        // Implement based on your ProductResponseDto structure
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
             //   .imageUrl(product.getImageUrl())
             //   .thumbnailUrl(product.getThumbnailUrl())
                .isActive(product.getIsActive())
                .build();
    }
}