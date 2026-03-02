package com.project.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.WishlistResponseDto;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.User;
import com.project.backend.entity.Wishlist;
import com.project.backend.entity.WishlistItems;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.DuplicateWishlistItemException;
import com.project.backend.exception.InsufficientStockException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.ProductVariantRepository;
import com.project.backend.repository.WishlistItemRepository;
import com.project.backend.repository.WishlistRepository;
import com.project.backend.requestDto.MoveToCartRequestDto;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.WishlistItemDto;
import com.project.backend.requestDto.WishlistRequestDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final WishlistItemRepository wishlistItemRepo;
    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;

    @Transactional
    public void add(User user, WishlistRequestDto request) {
        log.info("Adding to wishlist - User: {}, Product: {}, Variant: {}", 
                 user.getId(), request.getProductId(), request.getVariantId());

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        ProductVariant variant = variantRepo.findById(request.getVariantId())
                .orElseThrow(() -> new NotFoundException("Variant not found with id: " + request.getVariantId()));

        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BadRequestException("Variant does not belong to the specified product");
        }

        Wishlist wishlist = wishlistRepo.findByUser(user)
                .orElseGet(() -> createWishlist(user));

        if (wishlistItemRepo.existsByWishlistIdAndProductIdAndVariantId(
                wishlist.getId(), product.getId(), variant.getId())) {
            throw new DuplicateWishlistItemException(product.getId(), variant.getId());
        }


        WishlistItems item = WishlistItems.builder()
                .wishlist(wishlist)
                .product(product)
                .isActive(true)
                .variant(variant)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .priceAtAdd(variant.getSellingPrice().doubleValue())
                .build();

        wishlistItemRepo.save(item);
        log.info("Item added to wishlist successfully");
    }


    @Transactional
    public void remove(User user, Long productId, Long variantId) {
        log.info("Removing from wishlist - User: {}, Product: {}, Variant: {}", 
                 user.getId(), productId, variantId);
    if(wishlistItemRepo.findByWishlistIdAndProductIdAndVariantId(variantId, productId, variantId).isPresent()) {
        int deletedCount = wishlistItemRepo.deleteByWishlistIdAndProductIdAndVariantId(
                user.getId(), productId, variantId);
        if (deletedCount == 0) {
            throw new NotFoundException("Item not found in wishlist");
        }
    	
    }
    

        log.info("Item removed from wishlist");
    }

    @Transactional
    public void removeById(User user, Long itemId) {
        log.info("Removing wishlist item by ID: {} for user: {}", itemId, user.getId());

        WishlistItems item = wishlistItemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Wishlist item not found with id: " + itemId));

        if (!item.getWishlist().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to remove this item");
        }
        
        Wishlist wishlist = item.getWishlist();
        
        wishlistItemRepo.delete(item);
        
        long remainingItems = wishlistItemRepo.countByWishlistId(wishlist.getId());
        
        if (remainingItems == 0) {
            wishlistRepo.delete(wishlist);
            log.info("Wishlist {} deleted as it became empty", wishlist.getId());
        }
        
        log.info("Item removed from wishlist");
    }

 


    public PageResponseDto<WishlistItemDto> get(User user, int page, int size) {
        log.info("Fetching wishlist for user: {}, page: {}, size: {}", user.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<WishlistItems> itemsPage = wishlistItemRepo.findByWishlistUserId(user.getId(), pageable);

        List<WishlistItemDto> content = itemsPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResponseDto.<WishlistItemDto>builder()
                .content(content)
                .page(itemsPage.getNumber())
                .size(itemsPage.getSize())
                .totalElements(itemsPage.getTotalElements())
                .totalPages(itemsPage.getTotalPages())
                .last(itemsPage.isLast())
                .build();
    }

    public WishlistResponseDto getFullWishlist(User user) {
        log.info("Fetching full wishlist for user: {}", user.getId());

        Wishlist wishlist = wishlistRepo.findByUserWithItems(user)
                .orElseGet(() -> createWishlist(user));

        return mapToWishlistResponse(wishlist);
    }

    private Wishlist createWishlist(User user) {
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .isActive(true)
             
                .build();
        return wishlistRepo.save(wishlist);
    }

    private String getProductImage(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0).getImageUrl();
        }
        return null;
    }

    private String determineStockStatus(int available, int requested) {
        if (available == 0) return "OUT_OF_STOCK";
        if (available < requested) return "INSUFFICIENT_STOCK";
        if (available < 5) return "LOW_STOCK";
        return "IN_STOCK";
    }

    private int calculateDiscount(double sellingPrice, double mrp) {
        if (mrp == 0) return 0;
        return (int) Math.round(((mrp - sellingPrice) / mrp) * 100);
    }

    private WishlistItemDto mapToDto(WishlistItems item) {
        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        int availableStock = getAvailableStock(variant);
        int requestedQty = item.getQuantity();
        boolean inStock = availableStock >= requestedQty;
        String stockStatus = determineStockStatus(availableStock, requestedQty);

        return WishlistItemDto.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productBrand(product.getBrand())
                .productImage(getProductImage(product))
                .variantId(variant.getId())
                .variantSku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
//                .oldPrice(variant.getSellingPrice().doubleValue())
//                .mrp(variant.getMrp().doubleValue())
               .discountPercentage(calculateDiscount(
                        variant.getSellingPrice().doubleValue(),
                        variant.getMrp().doubleValue()))
                .availableStock(availableStock)
                .inStock(inStock)
     //           .stockStatus(stockStatus)
        //        .quantity(item.getQuantity())
                .addedAt(item.getCreatedAt())
         //       .priceAtAdd(item.getPriceAtAdd())
                .build();
    }

    private WishlistResponseDto mapToWishlistResponse(Wishlist wishlist) {
        List<WishlistItemDto> itemDtos = wishlist.getItems().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        double totalValue = 100; // itemDtos.stream()
//                .mapToDouble(i -> i.getPrice() * i.getQuantity())
//                .sum();

        long outOfStockCount = itemDtos.stream()
                .filter(i -> !i.getInStock())
                .count();

        return WishlistResponseDto.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .userName(wishlist.getUser().getName())
                .totalItems(itemDtos.size())
                .items(itemDtos)
                //.totalValue(totalValue)
             //   .outOfStockCount((int) outOfStockCount)
                .createdAt(wishlist.getCreatedAt())
                .updatedAt(wishlist.getUpdatedAt())
                .build();
    }
    
    
    
    
    @Transactional
    public void clearWishlist(User user) {
        log.info("Clearing entire wishlist for user: {}", user.getId());

        Wishlist wishlist = wishlistRepo.findByUser(user)
            .orElseThrow(() -> new NotFoundException("Wishlist not found for user"));

        wishlistItemRepo.deleteAllByWishlistId(wishlist.getId());
        
        wishlistRepo.delete(wishlist);
        
        log.info("Wishlist cleared successfully for user: {}", user.getId());
    }
    
    
    @Transactional
    public void moveToCart(User user, MoveToCartRequestDto req) {

        log.info("Moving {} items to cart for user: {}", req.getItemIds().size(), user.getId());

        if (req.getItemIds() == null || req.getItemIds().isEmpty()) {
            throw new BadRequestException("No items selected to move");
        }

        // Fetch all items
        List<WishlistItems> items = wishlistItemRepo.findAllById(req.getItemIds());

        if (items.size() != req.getItemIds().size()) {
            List<Long> foundIds = items.stream().map(WishlistItems::getId).toList();
            List<Long> missingIds = req.getItemIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new NotFoundException("Items not found with ids: " + missingIds);
        }

        // Track wishlists that will become empty
        Wishlist affectedWishlist = null;
        boolean allFromSameWishlist = true;

        // Verify all items belong to the user and check stock
        for (WishlistItems item : items) {
            // Verify ownership
            if (!item.getWishlist().getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("Item " + item.getId() + " does not belong to you");
            }

            // Track wishlist (all items should be from same wishlist in your design)
            if (affectedWishlist == null) {
                affectedWishlist = item.getWishlist();
            } else if (!affectedWishlist.getId().equals(item.getWishlist().getId())) {
                allFromSameWishlist = false;
            }

            // Check if variant exists and is active
            ProductVariant variant = item.getVariant();
            if (variant == null || !variant.getIsActive()) {
                throw new BadRequestException("Product variant is no longer available for item: " + item.getId());
            }

            // Check stock availability
            int availableStock = getAvailableStock(variant);
            if (availableStock < item.getQuantity()) {
                throw new InsufficientStockException(
                    item.getProduct().getName(),
                    availableStock,
                    item.getQuantity()
                );
            }
        }

      //   cartService.addItems(user.getId(), items);
        log.info("Successfully moved {} items to cart", items.size());

        // Remove from wishlist if requested
        if (req.getRemoveFromWishlist()) {
            // Delete all items
            wishlistItemRepo.deleteAllByIds(req.getItemIds());
            
            // If all items were from same wishlist, check if it's empty
            if (allFromSameWishlist && affectedWishlist != null) {
                long remainingItems = wishlistItemRepo.countByWishlistId(affectedWishlist.getId());
                if (remainingItems == 0) {
                    wishlistRepo.delete(affectedWishlist);
                    log.info("Wishlist {} deleted as it became empty", affectedWishlist.getId());
                }
            }
        }
    }

    // Helper method
    private int getAvailableStock(ProductVariant variant) {
        if (variant.getInventories() != null && !variant.getInventories().isEmpty()) {
            return variant.getInventories().stream()
                .mapToInt(wi -> wi.getAvailableQuantity() != null ? wi.getAvailableQuantity() : 0)
                .sum();
        }
        return 0;
    }
}