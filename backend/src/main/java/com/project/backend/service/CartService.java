package com.project.backend.service;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CartItemResponseDto;
import com.project.backend.ResponseDto.CartPricingResponseDto;
import com.project.backend.ResponseDto.MergeCartResultDto;
import com.project.backend.ResponseDto.MergeFailedItemDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.CartItem;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.ProductVariant;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.InsufficientStockException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.repository.CartItemRepository;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.ProductVariantRepository;
import com.project.backend.requestDto.CartMergeDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final  ProductVariantRepository variantRepo;
	private final ProductRepository productRepository;
	private final CartItemRepository cartItemRepository;

	@Transactional
	public CartItemResponseDto addToCart(User user, Long productId, Long variantId, Integer qty) {

	    if (qty == null || qty <= 0) {
	        throw new BadRequestException("Quantity must be greater than 0");
	    }
	    
	    Cart cart = cartRepository.findByUserId(user.getId())
	            .orElseGet(() -> {
	                Cart newCart = Cart.builder()
	                    .user(user)
	                    .totalQuantity(0)
	                    .totalAmount(0)
	                    .createdAt(Instant.now())
	                    .build();
	                return cartRepository.save(newCart);
	            });
	    
	    ProductVariant variant = variantRepo.findByIdWithProductAndInventories(variantId)
	        .orElseThrow(() -> new NotFoundException("Variant not found with id: " + variantId));
	    
	    Product product = productRepository.findById(productId)
	        .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
	    
	    if (!variant.getProduct().getId().equals(product.getId())) {
	        throw new BadRequestException("Variant does not belong to the specified product");
	    }

	    if (!Boolean.TRUE.equals(product.getIsActive())) {
	        throw new BadRequestException("Product is not available: " + product.getName());
	    }
	    Integer availableStock = getAvailableStock(variant); 
	    
//	    
//	    if (availableStock < qty) {
//	        throw new BadRequestException(
//	                String.format("Only %d units available for %s", availableStock, product.getName()));
//	    }
	    if (qty > 10) {
	        throw new BadRequestException("Maximum purchase quantity is 10 units");
	    }
	    CartItem existingItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
	        .orElse(null);

	    if (existingItem != null) {
	        // Update existing item
	        int newQuantity = existingItem.getQuantity() + qty;
	        
	        if (availableStock < newQuantity) {
	            throw new BadRequestException(
	                String.format("Cannot add %d more. Only %d available in stock", 
	                    qty, availableStock - existingItem.getQuantity()));
	        }
	        
	        existingItem.setQuantity(newQuantity);
	        existingItem.setPrice(variant.getSellingPrice().intValue());
	        cartItemRepository.save(existingItem);
	        
	        log.info("Updated cart item - User: {}, Product: {}, New Quantity: {}", 
	            user.getId(), productId, newQuantity);
	        
	    } else {
	        // Create new cart item
	        CartItem cartItem = CartItem.builder()
	            .cart(cart)
	            .product(product)
	            .variant(variant)
	            .quantity(qty)
	            .price(variant.getSellingPrice().intValue())
	            .build();
	        
	        cartItemRepository.save(cartItem);
	        
	        log.info("Added to cart - User: {}, Product: {}, Quantity: {}", 
	            user.getId(), productId, qty);
	    }
	    
	    updateCartTotals(cart);
	    
	    CartItem savedItem = existingItem != null ? existingItem : 
	        cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId).get();
	    
	    return mapToCartItemResponse(savedItem);
	}

	public List<CartItemResponseDto> getCart(User user) {

	    Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);
	    
	    if (cart == null || cart.getItems().isEmpty()) {
	        return new ArrayList<>(); 
	    }

	    return cart.getItems().stream()
	        .map(this::mapToCartItemResponse)
	        .collect(Collectors.toList());
	}
	private void updateCartTotals(Cart cart) {
	    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
	    
	    int totalQuantity = items.stream()
	        .mapToInt(CartItem::getQuantity)
	        .sum();
	    
	    int totalAmount = items.stream()
	        .mapToInt(item -> item.getPrice() * item.getQuantity())
	        .sum();
	    
	    cart.setTotalQuantity(totalQuantity);
	    cart.setTotalAmount(totalAmount);
	    cartRepository.save(cart);
	}
	private CartItemResponseDto mapToCartItemResponse(CartItem item) {
	    Product product = item.getProduct();
	    ProductVariant variant = item.getVariant();
	    
	    return CartItemResponseDto.builder()
	        .cartItemId(item.getId())
	        .productId(product.getId())
	        .productName(product.getName())
	        .variantId(variant.getId())
	        .size(variant.getSize())
	        .color(variant.getColor())
	        .imageUrl(getPrimaryImage(product))
	        .price((double) item.getPrice())
	        .quantity(item.getQuantity())
	        .totalPrice((double) (item.getPrice() * item.getQuantity()))
	        .build();
	}
	private Integer getAvailableStock(ProductVariant variant) {
	    if (variant.getInventories() != null && !variant.getInventories().isEmpty()) {
	        return variant.getInventories().stream()
	            .mapToInt(wi -> wi.getAvailableQuantity())
	            .sum();
	    }
	    return 0;
	}
	private String getPrimaryImage(Product product) {
		if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
			return null;
		}

		return product.getImages().stream().filter(img -> img.getIsPrimary() != null && img.getIsPrimary()).findFirst()
				.map(ProductImage::getImageUrl).orElse(product.getImages().get(0).getImageUrl());
	}

@Transactional
public void updateCartItem(User user, Long cartItemId, Integer qty, Integer newVariantId) {

    if (qty == null) {
        throw new BadRequestException("Quantity is required");
    }

    CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartItemId));

    if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedException("Unauthorized to modify this cart item");
    }

    Cart cart = cartItem.getCart();
    Product currentProduct = cartItem.getProduct();

    if (qty <= 0) {
        cartItemRepository.delete(cartItem);
        
        long remainingItems = cartItemRepository.countByCartId(cart.getId());
        if (remainingItems == 0) {
            cartRepository.delete(cart);
            log.info("Cart {} deleted as it became empty", cart.getId());
        } else {
            updateCartTotals(cart);
        }
        
        log.info("Removed cart item - User: {}, CartItem: {}", user.getId(), cartItemId);
        return;
    }

    if (newVariantId != null && !newVariantId.equals(cartItem.getVariant().getId())) {
        
        ProductVariant newVariant = variantRepo.findByIdWithInventories(newVariantId)
                .orElseThrow(() -> new NotFoundException("Variant not found with id: " + newVariantId));
        
        if (!newVariant.getProduct().getId().equals(currentProduct.getId())) {
            throw new BadRequestException("Variant does not belong to the same product");
        }
        
        boolean variantExists = cart.getItems().stream()
            .anyMatch(item -> !item.getId().equals(cartItemId) 
                && item.getVariant().getId().equals(newVariantId));
        
        if (variantExists) {
            CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(newVariantId))
                .findFirst()
                .get();
            
            int newQuantity = existingItem.getQuantity() + qty;
            
            Integer availableStock = getAvailableStock(newVariant);
            if (availableStock < newQuantity) {
                throw new BadRequestException(
                    String.format("Only %d units available for this variant", availableStock));
            }
            
            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(newVariant.getSellingPrice().intValue());
            cartItemRepository.save(existingItem);
            cartItemRepository.delete(cartItem);
            
            log.info("Merged cart item - User: {}, Old Variant: {}, New Variant: {}", 
                     user.getId(), cartItem.getVariant().getId(), newVariantId);
        } else {
            Integer availableStock = getAvailableStock(newVariant);
            if (availableStock < qty) {
                throw new BadRequestException(
                    String.format("Only %d units available for this variant", availableStock));
            }
            
            cartItem.setVariant(newVariant);
            cartItem.setQuantity(qty);
            cartItem.setPrice(newVariant.getSellingPrice().intValue());
            cartItemRepository.save(cartItem);
            
            log.info("Updated cart item variant - User: {}, CartItem: {}, New Variant: {}", 
                     user.getId(), cartItemId, newVariantId);
        }
    } else {
        ProductVariant variant = cartItem.getVariant();
        
        Integer availableStock = getAvailableStock(variant);
        if (availableStock < qty) {
            throw new BadRequestException(
                    String.format("Only %d units available for %s", availableStock, 
                                  variant.getProduct().getName()));
        }

        cartItem.setQuantity(qty);
        cartItem.setPrice(variant.getSellingPrice().intValue());
        cartItemRepository.save(cartItem);
        
        log.info("Updated cart item quantity - User: {}, CartItem: {}, New Quantity: {}", 
                 user.getId(), cartItemId, qty);
    }
    
    updateCartTotals(cart);
}

	@Transactional
	public void removeCartItem(User user, Long cartItemId) {

	    CartItem cartItem = cartItemRepository.findById(cartItemId)
	            .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartItemId));
	    if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
	        throw new UnauthorizedException("You don't have permission to remove this item");
	    }

	    Cart cart = cartItem.getCart();
	    
	    cartItemRepository.delete(cartItem);
	    
	    long remainingItems = cartItemRepository.countByCartId(cart.getId());
	    
	    if (remainingItems == 0) {
	        cartRepository.delete(cart);
	        log.info("Cart {} deleted as it became empty", cart.getId());
	    } else {
	        updateCartTotals(cart);
	    }

	    log.info("Removed cart item - User: {}, CartItem: {}", user.getId(), cartItemId);
	}

	@Transactional
	public MergeCartResultDto mergeCart(User user, List<CartMergeDto> items) {
	    if (items == null || items.isEmpty()) {
	        return MergeCartResultDto.builder()
	            .success(true)
	            .message("No items to merge")
	            .mergedCount(0)
	            .failedCount(0)
	            .build();
	    }

	    int successCount = 0;
	    int failCount = 0;
	    List<MergeFailedItemDto> failedItems = new ArrayList<>();

	    for (CartMergeDto item : items) {
	        try {
	            if (item.getVariantId() == null) {
	                throw new BadRequestException("Variant ID is required");
	            }
	            if (item.getQuantity() == null || item.getQuantity() <= 0) {
	                throw new BadRequestException("Quantity must be greater than 0");
	            }

	            addToCart(user, item.getProductId(), item.getVariantId(), item.getQuantity());
	            successCount++;
	            
	        } catch (NotFoundException e) {
	            log.warn("Product/Variant not found during merge - Product: {}, Variant: {}", 
	                     item.getProductId(), item.getVariantId());
	            failCount++;
	            failedItems.add(MergeFailedItemDto.builder()
	                .productId(item.getProductId())
	                .variantId(item.getVariantId())
	                .reason("Product not available")
	                .build());
	            
	        } catch (InsufficientStockException e) {
	            log.warn("Insufficient stock during merge - Product: {}", item.getProductId());
	            failCount++;
	            failedItems.add(MergeFailedItemDto.builder()
	                .productId(item.getProductId())
	                .variantId(item.getVariantId())
	                .reason(e.getMessage())
	                .build());
	            
	        } catch (Exception e) {
	            log.warn("Failed to merge item - Product: {}, Error: {}", 
	                     item.getProductId(), e.getMessage());
	            failCount++;
	            failedItems.add(MergeFailedItemDto.builder()
	                .productId(item.getProductId())
	                .variantId(item.getVariantId())
	                .reason("Failed to add to cart")
	                .build());
	        }
	    }

	    log.info("Merged {} items to cart for user: {} ({} succeeded, {} failed)", 
	             items.size(), user.getId(), successCount, failCount);

	    return MergeCartResultDto.builder()
	        .success(failCount == 0)
	        .message(failCount == 0 ? "All items merged successfully" : 
	                 String.format("%d items failed to merge", failCount))
	        .mergedCount(successCount)
	        .failedCount(failCount)
	        .failedItems(failedItems)
	        .build();
	}
	public CartPricingResponseDto getCartPricing(User user, String couponCode) {

	    Cart cart = cartRepository.findByUserId(user.getId())
	        .orElse(null);
	    
	    if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
	        return CartPricingResponseDto.builder()
	            .items(new ArrayList<>())
	            .subtotal(0.0)
	            .taxAmount(0.0)
	            .shippingCharges(0.0)
	            .discountAmount(0.0)
	            .finalAmount(0.0)
	            .couponApplied(false)
	            .message("Your cart is empty")
	            .totalItems(0)
	            .totalMrp(0.0)
	            .totalSavings(0.0)
	            .build();
	    }

	    double subtotal = 0.0;
	    double totalMrp = 0.0;
	    double tax = 0.0;
	    List<CartItemResponseDto> itemDtos = new ArrayList<>();

	    for (CartItem cartItem : cart.getItems()) {
	        Product product = cartItem.getProduct();
	        ProductVariant variant = cartItem.getVariant();

	        if (product == null) {
	            log.warn("Cart item {} has null product, skipping", cartItem.getId());
	            continue;
	        }

	        int qty = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

	        if (!Boolean.TRUE.equals(product.getIsActive())) {
	            throw new BadRequestException(product.getName() + " is not available");
	        }

	        Double price = 0.0;
	        Double mrp = 0.0;
	        
	        if (variant != null) {
	            price = variant.getSellingPrice() != null ? variant.getSellingPrice().doubleValue() : 0.0;
	            mrp = variant.getMrp() != null ? variant.getMrp().doubleValue() : price;
	        } else {
	            price = product.getMinPrice() != null ? product.getMinPrice() : 0.0;
	            mrp = product.getPrice() != null ? product.getPrice() : price;
	        }
	        
	        double itemTotal = price * qty;
	        double itemMrpTotal = mrp * qty;

	        double taxPercent = product.getTaxPercent() != null ? product.getTaxPercent() : 0.0;
	        double itemTax = (taxPercent / 100.0) * itemTotal;

	        subtotal += itemTotal;
	        totalMrp += itemMrpTotal;
	        tax += itemTax;

	        int discountPercentage = calculateDiscount(price, mrp);

	        itemDtos.add(CartItemResponseDto.builder()
	            .cartItemId(cartItem.getId())
	            .productId(product.getId())
	            .productName(product.getName())
	            .imageUrl(getPrimaryImage(product))
	            .variantId(variant != null ? variant.getId() : null)
	            .size(variant != null ? variant.getSize() : null)
	            .color(variant != null ? variant.getColor() : null)
	            .price(price)
	            .mrp(mrp)
	            .quantity(qty)
	            .totalPrice(itemTotal)
	            .discountPercentage(discountPercentage)
	            .build());
	    }

	    double shipping = calculateShipping(subtotal);
	    
	    double discount = 0.0;
	    boolean couponApplied = false;
	    String message = null;
	    
	    if (couponCode != null && !couponCode.isEmpty()) {
	        try {
	            discount = applyCoupon(couponCode, subtotal);
	            couponApplied = true;
	            message = "Coupon applied successfully";
	        } catch (Exception e) {
	            message = "Invalid coupon code: " + e.getMessage();
	        }
	    }

	    double finalAmount = subtotal + tax + shipping - discount;
	    double totalSavings = totalMrp - (subtotal - discount);

	    return CartPricingResponseDto.builder()
	        .items(itemDtos)
	        .totalItems(cart.getItems().size())
	        .subtotal(round(subtotal))
	        .totalMrp(round(totalMrp))
	        .taxAmount(round(tax))
	        .shippingCharges(shipping)
	        .discountAmount(round(discount))
	        .finalAmount(round(finalAmount))
	        .totalSavings(round(totalSavings))
	        .appliedCoupon(couponApplied ? couponCode : null)
	        .couponApplied(couponApplied)
	        .message(message)
	        .build();
	}
	private Double round(Double value) {
	    if (value == null) return 0.0;
	    return Math.round(value * 100.0) / 100.0;
	}

	private Double calculateShipping(Double subtotal) {
	    double FREE_SHIPPING_THRESHOLD = 999.0;
	    double STANDARD_SHIPPING = 49.0;
	    
	    if (subtotal == null || subtotal >= FREE_SHIPPING_THRESHOLD) {
	        return 0.0;
	    }
	    return STANDARD_SHIPPING;
	}

	private Double applyCoupon(String couponCode, Double subtotal) {

	    if ("SAVE10".equalsIgnoreCase(couponCode)) {
	        return subtotal * 0.10;
	    } else if ("SAVE50".equalsIgnoreCase(couponCode)) {
	        return 50.0;
	    }
	    throw new IllegalArgumentException("Invalid coupon code");
	}

	private Integer calculateDiscount(Double price, Double mrp) {
	    if (mrp == null || mrp == 0 || price == null) return 0;
	    return (int) Math.round(((mrp - price) / mrp) * 100);
	}
}