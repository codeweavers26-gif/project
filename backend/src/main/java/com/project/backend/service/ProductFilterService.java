package com.project.backend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.entity.Category;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.Section;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.ProductVariantRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.requestDto.BreadcrumbDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductFilterDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductFilterService {

	private final ProductRepository productRepo;
	private final CategoryRepository categoryRepo;
	private final ProductVariantRepository variantRepo;
	private final SectionRepository sectionRepository;


	public PageResponseDto<ProductResponseDto> filterProducts(ProductFilterDto filter) {
		 log.info("Filter params - sectionId: {}, categoryId: {}, minPrice: {}, maxPrice: {}, color: {}, size: {}, brand: {}", 
			        filter.getSectionId(), filter.getCategoryId(), filter.getMinPrice(), 
			        filter.getMaxPrice(), filter.getColor(), filter.getSize(), filter.getBrand());

	    Long categoryId = null;
	    Long sectionId = null;
	    
	    
	    if (filter.getCategoryId() != null && filter.getCategoryId() > 0) {
	        categoryId = filter.getCategoryId();
	        log.info("Using categoryId: {}", categoryId);
	    } else if (filter.getSectionId() != null && filter.getSectionId() > 0) {
	        sectionId = filter.getSectionId();
	        log.info("Using sectionId: {}", sectionId);
	    }
	    Pageable pageable = createPageable(filter);
	    log.info("Pageable: {}", pageable);
	    
	    String search = filter.getSearch();

	    if (search != null && !search.trim().isEmpty()) {
	        String[] words = search.toLowerCase().split("\\s+");
	        search = String.join("%", words); 
	    }
	    
	    Page<Object[]> productPage = productRepo.findActiveProductsWithFiltersNative(
	            categoryId,
	            sectionId, 
	            filter.getMinPrice(),
	            filter.getMaxPrice(),
	            filter.getSize(),
	            filter.getColor(),
	            filter.getBrands() != null && !filter.getBrands().isEmpty() ? filter.getBrands().get(0) : null,
	            		search, 
	            pageable
	    );
	    log.info("Query returned {} results, total elements: {}", 
	            productPage.getNumberOfElements(), productPage.getTotalElements());

	    List<ProductResponseDto> content = productPage.getContent().stream()
	            .map(row -> mapToProductDto(row))
	            .filter(dto -> dto != null)
	            .collect(Collectors.toList());

	    return PageResponseDto.<ProductResponseDto>builder()
	            .content(content)
	            .page(productPage.getNumber())
	            .size(productPage.getSize())
	            .totalElements(productPage.getTotalElements())
	            .totalPages(productPage.getTotalPages())
	            .last(productPage.isLast())
	            .build();
	}

	public Map<String, Object> getFilterOptions(Long sectionId, Long categoryId, Long subCategoryId) {

		Map<String, Object> options = new HashMap<>();
		Long targetId = determineCategoryId(sectionId, categoryId, subCategoryId);
		String currentLevel = getCurrentLevelName(sectionId, categoryId, subCategoryId);
		options.put("currentLevel", currentLevel);

		options.put("breadcrumb", getBreadcrumb(sectionId, categoryId, subCategoryId));

		List<String> brands = findDistinctBrands(targetId);
		options.put("brands", brands != null ? brands : new ArrayList<>());

		Map<String, Double> priceRange = getPriceRange(targetId);
		options.put("priceRange", priceRange);

		List<String> sizes = findDistinctSizes(targetId);
		options.put("sizes", sizes != null ? sizes : new ArrayList<>());

		List<String> colors = findDistinctColors(targetId);
		options.put("colors", colors != null ? colors : new ArrayList<>());

		Long totalProducts = countProducts(targetId);
		options.put("totalProducts", totalProducts);

		return options;
	}

	public List<BreadcrumbDto> getBreadcrumb(Long sectionId, Long categoryId, Long subCategoryId) {
		List<BreadcrumbDto> breadcrumb = new ArrayList<>();

		try {
			if (sectionId != null) {
				Section section = sectionRepository.findById(sectionId).orElse(null);

				if (section != null) {
					breadcrumb.add(BreadcrumbDto.builder().id(section.getId()).name(section.getName()) 
							.type("SECTION").url("/products?sectionId=" + section.getId()).build());
				}
			}

			if (categoryId != null) {
				Category category = categoryRepo.findById(categoryId).orElse(null);

				if (category != null) {
					breadcrumb.add(BreadcrumbDto.builder().id(category.getId()).name(category.getName()) 
																					
							.type("CATEGORY").url("/products?categoryId=" + category.getId()).build());
				}
			}

			if (subCategoryId != null) {
				Category subCategory = categoryRepo.findById(subCategoryId).orElse(null);

				if (subCategory != null) {
					breadcrumb.add(BreadcrumbDto.builder().id(subCategory.getId()).name(subCategory.getName())
							.type("SUBCATEGORY").url("/products?subCategoryId=" + subCategory.getId()).build());
				}
			}

		} catch (Exception e) {
			log.error("Error building breadcrumb: {}", e.getMessage());
		}

		return breadcrumb;
	}


	private Long determineCategoryId(Long sectionId, Long categoryId, Long subCategoryId) {
		if (subCategoryId != null) {
			return subCategoryId;
		}
		if (categoryId != null) {
			return categoryId;
		}
		if (sectionId != null) {
			return categoryRepo.findFirstBySectionId(sectionId).map(Category::getId).orElse(null);
		}
		return null;
	}

	private Pageable createPageable(ProductFilterDto filter) {
		int page = filter.getPage() != null ? filter.getPage() : 0;
		int size = filter.getLimit() != null ? filter.getLimit() : 20;

		String sortBy = filter.getSortBy();

		if (sortBy == null || sortBy.isEmpty()) {
			return PageRequest.of(page, size, Sort.by("created_at").descending());
		}

		switch (sortBy) {
		case "price_asc":
			return PageRequest.of(page, size, Sort.by("price").ascending());
		case "price_desc":
			return PageRequest.of(page, size, Sort.by("price").descending());
		case "name_asc":
			return PageRequest.of(page, size, Sort.by("name").ascending());
		case "name_desc":
			return PageRequest.of(page, size, Sort.by("name").descending());
		case "created_at":
			return PageRequest.of(page, size, Sort.by("created_at").descending());
		default:
			return PageRequest.of(page, size, Sort.by("created_at").descending());
		}
	}


	private String getCurrentLevelName(Long sectionId, Long categoryId, Long subCategoryId) {
		if (subCategoryId != null) {
			return categoryRepo.findById(subCategoryId).map(Category::getName).orElse("");
		} else if (categoryId != null) {
			return categoryRepo.findById(categoryId).map(Category::getName).orElse("");
		} else if (sectionId != null) {
			return categoryRepo.findById(sectionId).map(Category::getName).orElse("");
		}
		return "All Products";
	}

	private List<String> findDistinctBrands(Long categoryId) {
		if (categoryId == null) {
			return productRepo.findAll().stream().map(Product::getBrand).filter(Objects::nonNull).distinct()
					.collect(Collectors.toList());
		}

		return productRepo.findDistinctBrandsByCategoryId(categoryId);
	}

	private List<String> findDistinctSizes(Long categoryId) {
		if (categoryId == null) {
			return variantRepo.findAll().stream().map(ProductVariant::getSize).filter(Objects::nonNull).distinct()
					.collect(Collectors.toList());
		}

		return variantRepo.findDistinctSizesByCategoryId(categoryId);
	}

	private List<String> findDistinctColors(Long categoryId) {
		if (categoryId == null) {
			return variantRepo.findAll().stream().map(ProductVariant::getColor).filter(Objects::nonNull).distinct()
					.collect(Collectors.toList());
		}

		return variantRepo.findDistinctColorsByCategoryId(categoryId);
	}

	private Map<String, Double> getPriceRange(Long categoryId) {
		Map<String, Double> range = new HashMap<>();

		List<Object[]> priceRange;
		if (categoryId == null) {
			priceRange = variantRepo.findGlobalPriceRange();
		} else {
			priceRange = variantRepo.findPriceRangeByCategoryId(categoryId);
		}

		if (priceRange != null && !priceRange.isEmpty() && priceRange.get(0).length >= 2) {
			range.put("min", priceRange.get(0)[0] != null ? ((Number) priceRange.get(0)[0]).doubleValue() : 0.0);
			range.put("max", priceRange.get(0)[1] != null ? ((Number) priceRange.get(0)[1]).doubleValue() : 100000.0);
		} else {
			range.put("min", 0.0);
			range.put("max", 100000.0);
		}

		return range;
	}

	private Long countProducts(Long categoryId) {
		if (categoryId == null) {
			return productRepo.count();
		}
		return productRepo.countByCategoryId(categoryId);
	}

	private ProductResponseDto mapToProductDto(Object[] row) {
		if (row == null || row.length < 8)
			return null;

		try {
			Long productId = ((Number) row[0]).longValue();
			String name = (String) row[1];
			String slug = (String) row[2];
			String brand = (String) row[3];
			String shortDescription = (String) row[4];
			Double price = (Double) row[5];
			Integer stock = ((Number) row[6]).intValue();
			Boolean isActive = (Boolean) row[7];

			String thumbnail = row.length > 8 && row[8] != null ? (String) row[8] : null;
			Double minPrice = row.length > 9 && row[9] != null ? ((Number) row[9]).doubleValue() : price;

			ProductResponseDto.CategoryInfo categoryInfo = null;
			if (row.length > 12 && row[10] != null && row[11] != null && row[12] != null) {
				categoryInfo = ProductResponseDto.CategoryInfo.builder().id(((Number) row[10]).longValue())
						.name((String) row[11]).slug((String) row[12]).build();
			}

			return ProductResponseDto.builder().id(productId).name(name).slug(slug).brand(brand)
					.shortDescription(shortDescription).price(price).stock(stock).inStock(stock > 0).isActive(isActive)
					.mainImage(thumbnail).thumbnailImage(thumbnail).price(minPrice).category(categoryInfo).build();

		} catch (Exception e) {
			log.error("Error mapping product row to DTO: {}", e.getMessage());
			return null;
		}
	}

}