package com.project.backend.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.ResponseDto.ProductResponseDto;
import com.project.backend.entity.Category;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.Section;
import com.project.backend.entity.Warehouse;
import com.project.backend.entity.WarehouseInventory;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CategoryRepository;
import com.project.backend.repository.ProductImageRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.ProductVariantRepository;
import com.project.backend.repository.SectionRepository;
import com.project.backend.repository.WarehouseInventoryRepository;
import com.project.backend.repository.WarehouseRepository;
import com.project.backend.requestDto.BreadcrumbDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.ProductFilterDto;
import com.project.backend.requestDto.ProductRequestDto;
import com.project.backend.requestDto.VariantAvailabilityDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

	private final SectionRepository sectionRepository;
	private final ProductRepository productRepository;
	private final ProductVariantRepository variantRepository;
	private final ProductImageRepository productImageRepository;
	private final WarehouseInventoryRepository inventoryRepository;
	private final WarehouseRepository warehouseRepository;
	private final CategoryRepository categoryRepository;
	private final CloudinaryService cloudinaryService;
	private final SkuGeneratorService skuGeneratorService;

	@PersistenceContext
	private EntityManager entityManager;

	@Cacheable(value = "customerProducts", key = "#filter.toString()")
	public PageResponseDto<ProductResponseDto> getActiveProducts(ProductFilterDto filter) {

		log.info("Fetching active products with filters: {}", filter);

		Pageable pageable = createPageable(filter);

		Page<Object[]> productPage = productRepository.findActiveProductsWithFilters(filter.getCategoryId(),
				filter.getMinPrice(), filter.getMaxPrice(), filter.getSize(), filter.getColor(), filter.getBrand(),
				pageable);

		List<ProductResponseDto> dtos = productPage.getContent().stream().map(this::mapToCustomerProductDto)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return PageResponseDto.<ProductResponseDto>builder().content(dtos).page(productPage.getNumber())
				.size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
	}

	private ProductResponseDto mapToCustomerProductDto(Object[] row) {
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
			if (row.length > 12 && row[10] != null && row[11] != null) {
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

	@Transactional
	@CacheEvict(value = { "productsByLocation", "productDetails", "publicProducts",
			"variantAvailability" }, allEntries = true)
	public ProductResponseDto create(ProductRequestDto dto, List<MultipartFile> imageFiles) {

		if (imageFiles != null && imageFiles.size() > 6) {
			throw new BadRequestException("Maximum 6 images allowed per product");
		}

		Category category = categoryRepository.findById(dto.getCategoryId())
				.orElseThrow(() -> new NotFoundException("Category not found with id: " + dto.getCategoryId()));

		Product product = Product.builder().name(dto.getName()).brand(dto.getBrand())
				.shortDescription(dto.getShortDescription()).description(dto.getDescription()).category(category)
				.isActive(true).isDeleted(false)
				//.price(dto.getPrice())
				.deliveryDays(dto.getDeliveryDays())
				//.mrp(dto.getMrp())
				.codAvailable(dto.getCodAvailable())
			//	.discountPercent(dto.getDiscountPercent())
				.taxPercent(dto.getTaxPercent())
				.weight(dto.getWeight()).length(dto.getLength()).width(dto.getWidth()).height(dto.getHeight())
				.stock(dto.getStock() != null ? dto.getStock() : 0).build();

		product.setSlug(generateUniqueSlug(dto.getName()));

		Product savedProduct = productRepository.save(product);
		log.info("Product created with id: {}", savedProduct.getId());

		if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
			for (ProductRequestDto.VariantRequest vr : dto.getVariants()) {
				addVariantToProduct(savedProduct, vr);
			}
savedProduct.updateLowestPriceFromVariants();
        productRepository.save(savedProduct);

		}

		if (imageFiles != null && !imageFiles.isEmpty()) {
			uploadProductImages(savedProduct, imageFiles);
		}

		return getProductWithQueries(savedProduct.getId());
	}

	@Transactional
	@CacheEvict(value = { "productsByLocation", "productDetails", "publicProducts" }, allEntries = true)
	public ProductResponseDto update(Long id, ProductRequestDto dto) {

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

		product.setName(dto.getName());
		product.setBrand(dto.getBrand());
		product.setShortDescription(dto.getShortDescription());
		product.setDescription(dto.getDescription());

		if (dto.getCategoryId() != null) {
			Category category = categoryRepository.findById(dto.getCategoryId())
					.orElseThrow(() -> new NotFoundException("Category not found with id: " + dto.getCategoryId()));
			product.setCategory(category);
		}
		if (!product.getName().equals(dto.getName())) {
			product.setSlug(generateUniqueSlug(dto.getName()));
		}

		Product updatedProduct = productRepository.save(product);
		log.info("Product updated with id: {}", updatedProduct.getId());

		return getProductWithQueries(updatedProduct.getId());
	}

	@Transactional
	@CacheEvict(value = { "productsByLocation", "productDetails", "publicProducts" }, allEntries = true)
	public void softDeleteProduct(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

		product.setIsDeleted(true);
		product.setIsActive(false);
		productRepository.save(product);

		log.info("Product soft deleted with id: {}", id);
	}

	@Transactional
	@CacheEvict(value = { "productsByLocation", "productDetails", "publicProducts" }, allEntries = true)
	public void activateProduct(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

		boolean hasStock = inventoryRepository.existsByProductAndAvailableQuantityGreaterThan(id, 0);

		if (!hasStock) {
			throw new BadRequestException("Cannot activate product: No variants with stock");
		}

		product.setIsActive(true);
		productRepository.save(product);

		log.info("Product activated with id: {}", id);
	}

	public Page<ProductResponseDto> listProducts(Long categoryId, String status, Pageable pageable) {
		log.info("Listing products with filters - categoryId: {}, status: {}", categoryId, status);

		Page<Product> productPage = productRepository.findProductsByFilters(categoryId, status, pageable);

		return productPage.map(this::convertToListItemDto);
	}

	private ProductResponseDto convertToListItemDto(Product product) {

		String thumbnailImage = null;
		if (product.getImages() != null && !product.getImages().isEmpty()) {
			thumbnailImage = product.getImages().stream().sorted(Comparator.comparing(ProductImage::getPosition))
					.findFirst().map(ProductImage::getImageUrl).orElse(null);
		}

		Double minPrice = null;
		if (product.getVariants() != null && !product.getVariants().isEmpty()) {
			minPrice = product.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getIsActive()))
					.map(v -> v.getSellingPrice() != null ? v.getSellingPrice().doubleValue() : null)
					.filter(Objects::nonNull).min(Double::compareTo).orElse(null);
		}
Double displayPrice = minPrice != null ? minPrice : product.getPrice();
		Integer totalStock = 0;
		if (product.getVariants() != null) {
			totalStock = product.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getIsActive()))
					.mapToInt(v -> 1).sum();
		}

		 Instant createdAt = product.getCreatedAt() != null 
            ? product.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant() 
            : null;


		return ProductResponseDto.builder().id(product.getId()).name(product.getName()).slug(product.getSlug())
				.brand(product.getBrand()).shortDescription(product.getShortDescription()).price(product.getPrice())
				.stock(product.getStock()).inStock(product.getStock() != null && product.getStock() > 0)
				.isActive(product.getIsActive()).mainImage(thumbnailImage).thumbnailImage(thumbnailImage)
				  .mrp(product.getMrp())  .discountPercent(product.getDiscountPercent())
            .taxPercent(product.getTaxPercent())
            .weight(product.getWeight())
            .length(product.getLength())
            .width(product.getWidth())
            .height(product.getHeight())
            .codAvailable(product.getCodAvailable())
            .returnable(product.getReturnable())
            .deliveryDays(product.getDeliveryDays())
            .averageRating(product.getAverageRating())
            .totalReviews(product.getTotalReviews())
				.price(minPrice)
				.status(product.getStatus())
				.description(product.getDescription())
				.createdAt(createdAt)
				.category(product.getCategory() != null
						? ProductResponseDto.CategoryInfo.builder().id(product.getCategory().getId())
								.name(product.getCategory().getName()).slug(product.getCategory().getSlug()).build()
						: null)
				.build();
	}

	public ProductResponseDto getProduct(Long id) {
		log.info("Fetching product with id: {}", id);

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

		log.info("Product found: {}", product.getName());

		List<ProductVariant> variants = variantRepository.findByProductId(id);
		log.info("Found {} variants for product", variants.size());

		for (ProductVariant variant : variants) {
			List<WarehouseInventory> inventories = inventoryRepository.findByVariantId(variant.getId());
			log.info("Variant {} has {} inventory records", variant.getId(), inventories.size());

			variant.getInventories().clear();
			variant.getInventories().addAll(inventories);
		}

		product.getVariants().clear();
		product.getVariants().addAll(variants);

		List<ProductImage> images = productImageRepository.findByProductIdOrderByPositionAsc(id);
		log.info("Found {} images for product", images.size());

		product.getImages().clear();
		product.getImages().addAll(images);

		return mapToResponse(product);
	}

	@Transactional
	public ProductResponseDto addVariant(Long productId, ProductRequestDto.VariantRequest request) {

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

		if (variantRepository.existsByProductIdAndSizeAndColor(productId, request.getSize(), request.getColor())) {
			throw new BadRequestException(
					String.format("Variant with size '%s' and color '%s' already exists for this product",
							request.getSize(), request.getColor()));
		}

		addVariantToProduct(product, request);

		log.info("Variant added to product: {}", productId);

		return getProduct(productId);
	}

	@Transactional
	public ProductResponseDto updateVariant(Long variantId, ProductRequestDto.VariantRequest request) {

		ProductVariant variant = variantRepository.findById(variantId)
				.orElseThrow(() -> new NotFoundException("Variant not found with id: " + variantId));

		boolean exists = variantRepository.existsByProductIdAndSizeAndColorAndIdNot(variant.getProduct().getId(),
				request.getSize(), request.getColor(), variantId);

		if (exists) {
			throw new BadRequestException(String.format("Another variant with size '%s' and color '%s' already exists",
					request.getSize(), request.getColor()));
		}

		variant.setSize(request.getSize());
		variant.setColor(request.getColor());
		variant.setMrp(request.getMrp());
		variant.setSellingPrice(request.getSellingPrice());
		variant.setCostPrice(request.getCostPrice());

		variantRepository.save(variant);

		log.info("Variant updated with id: {}", variantId);

		return mapToResponse(variant.getProduct());
	}

	@Transactional
	public void deactivateVariant(Long variantId) {

		ProductVariant variant = variantRepository.findById(variantId)
				.orElseThrow(() -> new NotFoundException("Variant not found with id: " + variantId));

		variant.setIsActive(false);
		variantRepository.save(variant);

		log.info("Variant deactivated with id: {}", variantId);
	}

	@Transactional
	public ProductResponseDto addImages(Long productId, List<ProductRequestDto.ImageRequest> imageRequests) {

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

		int existingCount = product.getImages() != null ? product.getImages().size() : 0;
		if (existingCount + imageRequests.size() > 6) {
			throw new BadRequestException("Maximum 6 images allowed per product. Current: " + existingCount);
		}

		for (ProductRequestDto.ImageRequest ir : imageRequests) {
			ProductImage image = ProductImage.builder().product(product).imageUrl(ir.getImageUrl())
					.isPrimary(ir.getIsPrimary() != null ? ir.getIsPrimary() : false)
					.position(ir.getDisplayOrder() != null ? ir.getDisplayOrder() : existingCount + 1).build();

			productImageRepository.save(image);

			if (image.getIsPrimary()) {
				productImageRepository.unsetPrimaryForProduct(productId, image.getId());
			}
		}

		entityManager.refresh(product);

		log.info("Added {} images to product: {}", imageRequests.size(), productId);

		return mapToResponse(product);
	}

	@Transactional
	public ProductResponseDto uploadImages(Long productId, List<MultipartFile> imageFiles) {

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

		int existingCount = product.getImages() != null ? product.getImages().size() : 0;
		if (existingCount + imageFiles.size() > 6) {
			throw new BadRequestException("Maximum 6 images allowed per product. Current: " + existingCount);
		}

		uploadProductImages(product, imageFiles);

		entityManager.refresh(product);

		log.info("Uploaded {} images to product: {}", imageFiles.size(), productId);

		return mapToResponse(product);
	}

	@Transactional
	public void deleteImage(Long imageId) {

		ProductImage image = productImageRepository.findById(imageId)
				.orElseThrow(() -> new NotFoundException("Image not found with id: " + imageId));

		Long productId = image.getProduct().getId();

		if (image.getCloudinaryPublicId() != null) {
			try {
				cloudinaryService.deleteImage(image.getCloudinaryPublicId());
			} catch (Exception e) {
				log.error("Failed to delete image from Cloudinary: {}", image.getCloudinaryPublicId(), e);
			}
		}

		productImageRepository.delete(image);

		if (image.getIsPrimary()) {
			productImageRepository.findFirstByProductIdOrderByPositionAsc(productId).ifPresent(img -> {
				img.setIsPrimary(true);
				productImageRepository.save(img);
			});
		}

		log.info("Image deleted with id: {}", imageId);
	}

	@Transactional
	public ProductResponseDto setPrimaryImage(Long imageId) {

		ProductImage image = productImageRepository.findById(imageId)
				.orElseThrow(() -> new NotFoundException("Image not found with id: " + imageId));

		Long productId = image.getProduct().getId();

		productImageRepository.unsetPrimaryForProduct(productId, null);

		image.setIsPrimary(true);
		productImageRepository.save(image);

		log.info("Image set as primary with id: {}", imageId);

		return mapToResponse(image.getProduct());
	}

	@Transactional
	public void bulkStockUpdate(List<ProductRequestDto.StockUpdateRequest> stockUpdates) {

		for (ProductRequestDto.StockUpdateRequest request : stockUpdates) {
			WarehouseInventory inventory = inventoryRepository
					.findByVariantIdAndWarehouseId(request.getVariantId(), request.getWarehouseId())
					.orElseThrow(() -> new NotFoundException("Inventory not found for variant: "
							+ request.getVariantId() + " and warehouse: " + request.getWarehouseId()));

			inventory.setAvailableQuantity(request.getQuantity());
			inventoryRepository.save(inventory);
		}

		log.info("Bulk stock updated for {} variants", stockUpdates.size());
	}

	@Cacheable(value = "publicProducts", key = "#page + '_' + #size + '_' + #sortBy + '_' + #categoryId")
	public PageResponseDto<ProductResponseDto> getActiveProducts(int page, int size, String sortBy, Long categoryId) {

		Sort sort = getSafeSort(sortBy);
		PageRequest pageable = PageRequest.of(page, size, sort);

		Page<Product> productPage = productRepository.findActiveProductsWithStock(categoryId, pageable);

		return mapToPageResponse(productPage);
	}

	@Cacheable(value = "productBySlug", key = "#slug")
	public ProductResponseDto getProductBySlug(String slug) {
		Product product = productRepository.findBySlugAndIsActiveTrue(slug)
				.orElseThrow(() -> new NotFoundException("Product not found with slug: " + slug));
		return mapToResponse(product);
	}

	@Cacheable(value = "variantAvailability", key = "#productId")
	public List<VariantAvailabilityDto> getVariantAvailability(Long productId) {
		List<Object[]> results = variantRepository.findAvailabilityRaw(productId);

		return results.stream()
				.map(row -> new VariantAvailabilityDto((Long) row[0], (String) row[1], (String) row[2], (String) row[3],
						(Boolean) row[4], ((Number) row[5]).intValue(), (BigDecimal) row[6]))
				.collect(Collectors.toList());
	}

	private void addVariantToProduct(Product product, ProductRequestDto.VariantRequest vr) {
		try {
			String sku = skuGeneratorService.generateSku(product.getBrand() != null ? product.getBrand() : "R&R",
					product.getCategory() != null ? product.getCategory().getName() : "GEN", vr.getColor(),
					vr.getSize());

  BigDecimal sellingPrice = calculateSellingPrice(
                vr.getCostPrice(),
                vr.getProfitMargin(),
                product.getTaxPercent() != null ? product.getTaxPercent() : 0.0
        );



			ProductVariant variant = ProductVariant.builder().product(product).sku(sku).size(vr.getSize())
					.color(vr.getColor()).mrp(vr.getMrp())
					   .profitMargin(vr.getProfitMargin()) 
                .sellingPrice(sellingPrice)         
					.costPrice(vr.getCostPrice()).isActive(true).build();

			ProductVariant savedVariant = variantRepository.save(variant);
			log.info("Variant created with SKU: {}", sku);

			Warehouse defaultWarehouse = warehouseRepository.findDefaultWarehouse()
					.orElseThrow(() -> new NotFoundException("Default warehouse not configured"));

			WarehouseInventory inventory = WarehouseInventory.builder().warehouse(defaultWarehouse)
					.variant(savedVariant).availableQuantity(vr.getInitialStock()).reservedQuantity(0).build();

			inventoryRepository.save(inventory);
			log.info("Inventory created for variant {} with stock {}", savedVariant.getId(), vr.getInitialStock());

			product.getVariants().add(savedVariant);

		} catch (Exception e) {
			log.error("Failed to add variant to product {}: {}", product.getId(), e.getMessage());
			throw new RuntimeException("Failed to add variant: " + e.getMessage(), e);
		}
	}

private BigDecimal calculateSellingPrice(BigDecimal costPrice, BigDecimal profitMargin, Double taxPercent) {
    if (costPrice == null || profitMargin == null) {
        throw new BadRequestException("Cost price and profit margin are required");
    }
    
    BigDecimal profitMultiplier = profitMargin
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    
    BigDecimal profitAmount = costPrice.multiply(profitMultiplier);
    BigDecimal priceAfterMargin = costPrice.add(profitAmount);
    
    if (taxPercent != null && taxPercent > 0) {
        BigDecimal taxMultiplier = BigDecimal.valueOf(taxPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxAmount = priceAfterMargin.multiply(taxMultiplier);
        priceAfterMargin = priceAfterMargin.add(taxAmount);
    }
    
    return priceAfterMargin.setScale(2, RoundingMode.HALF_UP);
}


	private void uploadProductImages(Product product, List<MultipartFile> imageFiles) {
		int position = 1;

		if (product.getImages() != null && !product.getImages().isEmpty()) {
			position = product.getImages().stream().mapToInt(ProductImage::getPosition).max().orElse(0) + 1;
		}

		for (MultipartFile imageFile : imageFiles) {
			try {
				Map uploadResult = cloudinaryService.uploadImage(imageFile, "products/" + product.getId());

				ProductImage image = ProductImage.builder().product(product)
						.imageUrl((String) uploadResult.get("secure_url"))
						.cloudinaryPublicId((String) uploadResult.get("public_id")).position(position++)
						.isPrimary(false).build();

				productImageRepository.save(image);

			} catch (IOException e) {
				log.error("Failed to upload image: {}", imageFile.getOriginalFilename(), e);
				throw new RuntimeException("Failed to upload image: " + imageFile.getOriginalFilename(), e);
			}
		}

		if (product.getImages() == null || product.getImages().isEmpty()) {
			productImageRepository.findFirstByProductIdOrderByPositionAsc(product.getId()).ifPresent(img -> {
				img.setIsPrimary(true);
				productImageRepository.save(img);
			});
		}
	}

	private String generateUniqueSlug(String name) {
		String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-");

		String slug = baseSlug;
		int counter = 1;

		while (productRepository.existsBySlug(slug)) {
			slug = baseSlug + "-" + counter++;
		}

		return slug;
	}

	private ProductResponseDto mapToResponse(Product product) {

		log.info("Mapping product ID: {} to response", product.getId());

		List<ProductVariant> variants = product.getVariants();
		log.info("Product has {} variants in entity", variants != null ? variants.size() : 0);

		List<ProductResponseDto.VariantInfo> variantInfos = new ArrayList<>();

		if (variants != null && !variants.isEmpty()) {
			variantInfos = variants.stream().filter(v -> Boolean.TRUE.equals(v.getIsActive())).map(v -> {
				Integer availableStock = 0;
				if (v.getInventories() != null && !v.getInventories().isEmpty()) {
					availableStock = v.getInventories().stream()
							.mapToInt(wi -> wi.getAvailableQuantity() != null ? wi.getAvailableQuantity() : 0).sum();
				}

				return ProductResponseDto.VariantInfo.builder().id(v.getId()).sku(v.getSku()).size(v.getSize())
						.color(v.getColor()).mrp(v.getMrp()).sellingPrice(v.getSellingPrice()).isActive(v.getIsActive())
						.availableStock(availableStock).build();
			}).collect(Collectors.toList());
		}

		Integer totalStock = variantInfos.stream().mapToInt(ProductResponseDto.VariantInfo::getAvailableStock).sum();

		Double minPrice = variantInfos.stream().map(ProductResponseDto.VariantInfo::getSellingPrice)
				.filter(Objects::nonNull).map(BigDecimal::doubleValue).min(Double::compareTo).orElse(0.0);

		List<ProductResponseDto.ImageInfo> imageInfos = new ArrayList<>();
		if (product.getImages() != null && !product.getImages().isEmpty()) {
			imageInfos = product.getImages().stream().sorted(Comparator.comparing(ProductImage::getPosition))
					.map(img -> ProductResponseDto.ImageInfo.builder().id(img.getId()).imageUrl(img.getImageUrl())
							.isPrimary(img.getIsPrimary() != null ? img.getIsPrimary() : false)
							.position(img.getPosition()).build())
					.collect(Collectors.toList());
		}

		String mainImage = imageInfos.stream().filter(ProductResponseDto.ImageInfo::getIsPrimary).findFirst()
				.map(ProductResponseDto.ImageInfo::getImageUrl)
				.orElse(imageInfos.isEmpty() ? null : imageInfos.get(0).getImageUrl());

		ProductResponseDto.CategoryInfo categoryInfo = null;
		if (product.getCategory() != null) {
			categoryInfo = ProductResponseDto.CategoryInfo.builder().id(product.getCategory().getId())
					.name(product.getCategory().getName()).slug(product.getCategory().getSlug()).build();
		}

		Instant createdAt = null;
		if (product.getCreatedAt() != null) {
			createdAt = product.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
		}

		ProductResponseDto response = ProductResponseDto.builder().id(product.getId()).name(product.getName())
				.slug(product.getSlug()).brand(product.getBrand()).shortDescription(product.getShortDescription())
				.description(product.getDescription()).isActive(product.getIsActive()).createdAt(createdAt)
				.category(categoryInfo).variants(variantInfos).images(imageInfos).mainImage(mainImage)
				.thumbnailImage(mainImage).mediumImage(mainImage).stock(totalStock).inStock(totalStock > 0)
				.stockStatus(totalStock > 0 ? (totalStock < 5 ? "LOW_STOCK" : "IN_STOCK") : "OUT_OF_STOCK")
				// .minPrice(minPrice)
				.build();

		log.info("Response built with {} variants and {} images",
				response.getVariants() != null ? response.getVariants().size() : 0,
				response.getImages() != null ? response.getImages().size() : 0);

		return response;
	}

	private Long determineCategoryId(Long sectionId, Long categoryId, Long subCategoryId) {

		if (subCategoryId != null) {
			return subCategoryId;
		}
		if (categoryId != null) {
			return categoryId;
		}
		if (sectionId != null) {
			return categoryRepository.findFirstBySectionId(sectionId).map(Category::getId).orElse(null);
		}
		return null;
	}

	private List<String> findDistinctSizes(Long targetId) {
		if (targetId == null)
			return new ArrayList<>();

		boolean isSection = sectionRepository.existsById(targetId);

		if (isSection) {
			List<Category> categories = categoryRepository.findBySectionId(targetId);
			List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

			return entityManager.createQuery(
					"SELECT DISTINCT v.size FROM ProductVariant v WHERE v.product.category.id IN :categoryIds",
					String.class).setParameter("categoryIds", categoryIds).getResultList();
		} else {
			return entityManager.createQuery(
					"SELECT DISTINCT v.size FROM ProductVariant v WHERE v.product.category.id = :categoryId",
					String.class).setParameter("categoryId", targetId).getResultList();
		}
	}

	private PageResponseDto<ProductResponseDto> mapToPageResponse(Page<Product> productPage) {
		return PageResponseDto.<ProductResponseDto>builder()
				.content(productPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
				.page(productPage.getNumber()).size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
	}

	public PageResponseDto<ProductResponseDto> getActiveProducts(int page, int size, String sortBy) {
		Sort sort = getSafeSort(sortBy);
		PageRequest pageable = PageRequest.of(page, size, sort);

		Page<Product> productPage = productRepository.findActiveProductsWithStock(null, pageable);

		return mapToPageResponse(productPage);
	}

	@Cacheable(value = "productDetails", key = "#id")
	public ProductResponseDto getActiveProductById(Long id) {
		Product product = productRepository.findByIdAndIsActiveTrue(id)
				.orElseThrow(() -> new NotFoundException("Product not found or inactive with id: " + id));
		return mapToResponse(product);
	}

	@Cacheable(value = "productsByLocation", key = "#locationId + '_' + #page + '_' + #size")
	public PageResponseDto<ProductResponseDto> getProductsByLocation(Long locationId, int page, int size) {

		if (!warehouseRepository.existsByLocationId(locationId)) {
			throw new NotFoundException("Location not found with id: " + locationId);
		}

		PageRequest pageable = PageRequest.of(page, size);
		Page<Product> products = productRepository.findAvailableInLocation(locationId, pageable);

		return mapToPageResponse(products);
	}

	public ProductResponseDto convertToResponse(Product product) {
		return mapToResponse(product);
	}

	private Sort getSafeSort(String sortBy) {
		if (sortBy == null)
			return Sort.by("createdAt").descending();

		switch (sortBy) {
		case "price_asc":
			return Sort.by("variants.sellingPrice").ascending();
		case "price_desc":
			return Sort.by("variants.sellingPrice").descending();
		case "name_asc":
			return Sort.by("name").ascending();
		case "name_desc":
			return Sort.by("name").descending();
		case "createdAt":
		default:
			return Sort.by("createdAt").descending();
		}
	}

	public ProductResponseDto getProductWithQueries(Long id) {
		log.info("Fetching product with id: {} using separate queries", id);

		List<Object[]> basicResults = productRepository.findProductBasic(id);
		if (basicResults.isEmpty()) {
			throw new NotFoundException("Product not found with id: " + id);
		}

		Object[] row = basicResults.get(0);

		ProductResponseDto.ProductResponseDtoBuilder builder = ProductResponseDto.builder()
				.id(((Number) row[0]).longValue()).name((String) row[1]).slug((String) row[2]).brand((String) row[3])
				.shortDescription((String) row[4]).description((String) row[5]).price((Double) row[6])
				.stock(((Number) row[7]).intValue()).isActive((Boolean) row[8]);

		if (row[11] != null) {
			ProductResponseDto.CategoryInfo category = ProductResponseDto.CategoryInfo.builder()
					.id(((Number) row[11]).longValue()).name((String) row[12]).slug((String) row[13]).build();
			builder.category(category);
		}

		List<Object[]> variantResults = productRepository.findVariantsByProductId(id);
		List<ProductResponseDto.VariantInfo> variantInfos = new ArrayList<>();
		int totalStock = 0;
		Double minPrice = Double.MAX_VALUE;

		for (Object[] vRow : variantResults) {
			if (vRow[0] == null)
				continue;

			ProductResponseDto.VariantInfo variant = ProductResponseDto.VariantInfo.builder()
					.id(((Number) vRow[0]).longValue()).sku((String) vRow[1]).size((String) vRow[2])
					.color((String) vRow[3]).mrp((BigDecimal) vRow[4]).sellingPrice((BigDecimal) vRow[5])
					.costPrice((BigDecimal) vRow[6]).isActive((Boolean) vRow[7])
					.availableStock(((Number) vRow[8]).intValue()).build();

			variantInfos.add(variant);
			totalStock += ((Number) vRow[8]).intValue();

			if (variant.getSellingPrice() != null) {
				minPrice = Math.min(minPrice, variant.getSellingPrice().doubleValue());
			}
		}
		builder.variants(variantInfos);
		builder.stock(totalStock);
		builder.inStock(totalStock > 0);
		builder.price(minPrice != Double.MAX_VALUE ? minPrice : 0.0);

		List<Object[]> imageResults = productRepository.findImagesByProductId(id);
		List<ProductResponseDto.ImageInfo> imageInfos = new ArrayList<>();
		String mainImage = null;

		for (Object[] iRow : imageResults) {
			if (iRow[0] == null)
				continue;

			Long imageId = ((Number) iRow[0]).longValue();
			String imageUrl = (String) iRow[1];
			Boolean isPrimary = (Boolean) iRow[2];
			Integer position = iRow[3] != null ? ((Number) iRow[3]).intValue() : 0;

			ProductResponseDto.ImageInfo imageInfo = ProductResponseDto.ImageInfo.builder().id(imageId)
					.imageUrl(imageUrl).isPrimary(isPrimary).position(position).build();

			imageInfos.add(imageInfo);

			if (isPrimary != null && isPrimary) {
				mainImage = imageUrl;
			}
		}

		builder.images(imageInfos);

		String finalMainImage = mainImage != null ? mainImage
				: (imageInfos.isEmpty() ? null : imageInfos.get(0).getImageUrl());
		builder.mainImage(finalMainImage);
		builder.thumbnailImage(finalMainImage);
		builder.mediumImage(finalMainImage);

		if (totalStock <= 0) {
			builder.stockStatus("OUT_OF_STOCK");
		} else if (totalStock < 5) {
			builder.stockStatus("LOW_STOCK");
		} else {
			builder.stockStatus("IN_STOCK");
		}

		ProductResponseDto dto = builder.build();

		log.info("Found {} variants and {} images", variantInfos.size(), imageInfos.size());

		return dto;
	}

	private Pageable createPageable(ProductFilterDto filter) {
		Sort sort = getSort(filter.getSortBy());
		return PageRequest.of(filter.getPage() != null ? filter.getPage() : 0,
				filter.getSize() != null ? filter.getLimit() : 20, sort);
	}

	private Sort getSort(String sortBy) {
		if (sortBy == null)
			return Sort.by("createdAt").descending();

		switch (sortBy) {
		case "price_asc":
			return Sort.by("minPrice").ascending();
		case "price_desc":
			return Sort.by("minPrice").descending();
		case "name_asc":
			return Sort.by("name").ascending();
		case "name_desc":
			return Sort.by("name").descending();
		case "popularity":
			return Sort.by("totalSold").descending();
		default:
			return Sort.by("createdAt").descending();
		}

	}

	public PageResponseDto<ProductResponseDto> getFeaturedProducts(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Object[]> productPage = productRepository.findFeaturedProducts(pageable);

		List<ProductResponseDto> content = productPage.getContent().stream().map(this::mapToBestSellerDto)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return PageResponseDto.<ProductResponseDto>builder().content(content).page(productPage.getNumber())
				.size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
	}

	public PageResponseDto<ProductResponseDto> getNewArrivals(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Object[]> productPage = productRepository.findNewArrivals(pageable);
		List<ProductResponseDto> content = productPage.getContent().stream().map(this::mapToBestSellerDto)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return PageResponseDto.<ProductResponseDto>builder().content(content).page(productPage.getNumber())
				.size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
	}

	private ProductResponseDto mapToBestSellerDto(Object[] row) {
		if (row == null || row.length < 17)
			return null;

		try {
			int index = 0;

			Long productId = ((Number) row[index++]).longValue();
			String status = (String) row[index++];
			String description = (String) row[index++];
			Double mrp = (Double) row[index++];
			Boolean returnable = (Boolean) row[index++];
			String name = (String) row[index++];
			String slug = (String) row[index++];
			String brand = (String) row[index++];
			String shortDescription = (String) row[index++];
			Double price = (Double) row[index++];
			Integer stock = ((Number) row[index++]).intValue();
			Boolean isActive = (Boolean) row[index++];
			String thumbnail = (String) row[index++];
			Double minPrice = row[index] != null ? ((Number) row[index]).doubleValue() : price;
			index++;
			Long categoryId = row[index] != null ? ((Number) row[index]).longValue() : null;
			index++;
			String categoryName = (String) row[index++];
			String categorySlug = (String) row[index++];

			ProductResponseDto.CategoryInfo categoryInfo = null;
			if (categoryId != null) {
				categoryInfo = ProductResponseDto.CategoryInfo.builder().id(categoryId).name(categoryName)
						.slug(categorySlug).build();
			}

			String stockStatus = "OUT_OF_STOCK";
			if (stock > 0) {
				stockStatus = stock < 5 ? "LOW_STOCK" : "IN_STOCK";
			}

			return ProductResponseDto.builder().id(productId).name(name).slug(slug).brand(brand)
					.shortDescription(shortDescription).description(description).price(price).mrp(mrp).stock(stock)
					.inStock(stock > 0).stockStatus(stockStatus).isActive(isActive).returnable(returnable)
					.status(status).mainImage(thumbnail).thumbnailImage(thumbnail).price(minPrice)
					.category(categoryInfo).build();

		} catch (Exception e) {
			log.error("Error mapping best seller row to DTO: {}", e.getMessage());
			return null;
		}
	}

	public PageResponseDto<ProductResponseDto> getBestSellers(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Object[]> productPage = productRepository.findBestSellers(pageable);

		List<ProductResponseDto> content = productPage.getContent().stream().map(this::mapToBestSellerDto)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return PageResponseDto.<ProductResponseDto>builder().content(content).page(productPage.getNumber())
				.size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
	}
	
	public PageResponseDto<ProductResponseDto> getRelatedProducts(Long productId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Object[]> productPage = productRepository.findRelatedProducts(productId, pageable);
		List<ProductResponseDto> content = productPage.getContent().stream().map(this::mapToBestSellerDto)
				.filter(Objects::nonNull).collect(Collectors.toList());

		return PageResponseDto.<ProductResponseDto>builder().content(content).page(productPage.getNumber())
				.size(productPage.getSize()).totalElements(productPage.getTotalElements())
				.totalPages(productPage.getTotalPages()).last(productPage.isLast()).build();
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

	private String getCurrentLevelName(Long sectionId, Long categoryId, Long subCategoryId) {
		if (subCategoryId != null) {
			return categoryRepository.findById(subCategoryId).map(Category::getName).orElse("Products");
		}
		if (categoryId != null) {
			return categoryRepository.findById(categoryId).map(Category::getName).orElse("Products");
		}
		if (sectionId != null) {
			return sectionRepository.findById(sectionId).map(Section::getName).orElse("Products");
		}
		return "All Products";
	}

	private List<BreadcrumbDto> getBreadcrumb(Long sectionId, Long categoryId, Long subCategoryId) {
		List<BreadcrumbDto> breadcrumb = new ArrayList<>();

		if (sectionId != null) {
			sectionRepository.findById(sectionId).ifPresent(
					section -> breadcrumb.add(BreadcrumbDto.builder().id(section.getId()).name(section.getName())
							.type("SECTION").url("/products?sectionId=" + section.getId()).build()));
		}

		if (categoryId != null) {
			categoryRepository.findById(categoryId).ifPresent(
					category -> breadcrumb.add(BreadcrumbDto.builder().id(category.getId()).name(category.getName())
							.type("CATEGORY").url("/products?categoryId=" + category.getId()).build()));
		}

		if (subCategoryId != null) {
			categoryRepository.findById(subCategoryId)
					.ifPresent(subCategory -> breadcrumb.add(BreadcrumbDto.builder().id(subCategory.getId())
							.name(subCategory.getName()).type("SUBCATEGORY")
							.url("/products?subCategoryId=" + subCategory.getId()).build()));
		}

		return breadcrumb;
	}

	private List<String> findDistinctBrands(Long targetId) {
		if (targetId == null)
			return new ArrayList<>();

		boolean isSection = sectionRepository.existsById(targetId);

		if (isSection) {
			List<Category> categories = categoryRepository.findBySectionId(targetId);
			List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

			if (categoryIds.isEmpty())
				return new ArrayList<>();

			return entityManager.createQuery(
					"SELECT DISTINCT p.brand FROM Product p WHERE p.category.id IN :categoryIds AND p.brand IS NOT NULL",
					String.class).setParameter("categoryIds", categoryIds).getResultList();
		} else {
			return entityManager.createQuery(
					"SELECT DISTINCT p.brand FROM Product p WHERE p.category.id = :categoryId AND p.brand IS NOT NULL",
					String.class).setParameter("categoryId", targetId).getResultList();
		}
	}

	private Map<String, Double> getPriceRange(Long targetId) {
		Map<String, Double> priceRange = new HashMap<>();
		priceRange.put("min", 0.0);
		priceRange.put("max", 100000.0);

		if (targetId == null)
			return priceRange;

		try {
			boolean isSection = sectionRepository.existsById(targetId);
			List<Double> prices = new ArrayList<>();

			if (isSection) {
				List<Category> categories = categoryRepository.findBySectionId(targetId);
				List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

				if (!categoryIds.isEmpty()) {
					prices = entityManager.createQuery(
							"SELECT MIN(v.sellingPrice), MAX(v.sellingPrice) FROM ProductVariant v WHERE v.product.category.id IN :categoryIds",
							Object[].class).setParameter("categoryIds", categoryIds).getResultList().stream()
							.flatMap(arr -> Stream.of(((Number) arr[0]).doubleValue(), ((Number) arr[1]).doubleValue()))
							.collect(Collectors.toList());
				}
			} else {
				Object[] result = entityManager.createQuery(
						"SELECT MIN(v.sellingPrice), MAX(v.sellingPrice) FROM ProductVariant v WHERE v.product.category.id = :categoryId",
						Object[].class).setParameter("categoryId", targetId).getSingleResult();

				if (result[0] != null)
					prices.add(((Number) result[0]).doubleValue());
				if (result[1] != null)
					prices.add(((Number) result[1]).doubleValue());
			}

			if (!prices.isEmpty()) {
				priceRange.put("min", prices.stream().min(Double::compare).orElse(0.0));
				priceRange.put("max", prices.stream().max(Double::compare).orElse(100000.0));
			}
		} catch (Exception e) {
			log.error("Error getting price range: {}", e.getMessage());
		}

		return priceRange;
	}

	private List<String> findDistinctColors(Long targetId) {
		if (targetId == null)
			return new ArrayList<>();

		boolean isSection = sectionRepository.existsById(targetId);

		if (isSection) {
			List<Category> categories = categoryRepository.findBySectionId(targetId);
			List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

			if (categoryIds.isEmpty())
				return new ArrayList<>();

			return entityManager.createQuery(
					"SELECT DISTINCT v.color FROM ProductVariant v WHERE v.product.category.id IN :categoryIds AND v.color IS NOT NULL",
					String.class).setParameter("categoryIds", categoryIds).getResultList();
		} else {
			return entityManager.createQuery(
					"SELECT DISTINCT v.color FROM ProductVariant v WHERE v.product.category.id = :categoryId AND v.color IS NOT NULL",
					String.class).setParameter("categoryId", targetId).getResultList();
		}
	}

	private Long countProducts(Long targetId) {
		if (targetId == null)
			return 0L;

		boolean isSection = sectionRepository.existsById(targetId);

		if (isSection) {
			List<Category> categories = categoryRepository.findBySectionId(targetId);
			List<Long> categoryIds = categories.stream().map(Category::getId).collect(Collectors.toList());

			if (categoryIds.isEmpty())
				return 0L;

			return entityManager.createQuery(
					"SELECT COUNT(DISTINCT p) FROM Product p WHERE p.category.id IN :categoryIds AND p.isActive = true",
					Long.class).setParameter("categoryIds", categoryIds).getSingleResult();
		} else {
			return entityManager.createQuery(
					"SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true",
					Long.class).setParameter("categoryId", targetId).getSingleResult();
		}
	}
}
