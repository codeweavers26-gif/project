package com.project.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.AdminCartItemDto;
import com.project.backend.ResponseDto.AdminCartSummaryDto;
import com.project.backend.ResponseDto.CartStatisticsDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.CartItem; // Make sure you have this entity
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CartItemRepository; // Add this
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository; // Add this
    private final UserRepository userRepository;

    /**
     * Get paginated cart items with filters
     */
    public PageResponseDto<AdminCartItemDto> getCartItems(
            Long userId, Long productId, String userEmail, String userName,
            String productName, Integer minQuantity, Integer maxQuantity,
            Instant fromDate, Instant toDate, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // Fix: Use CartItemRepository instead of CartRepository
        Page<CartItem> cartItemPage = cartItemRepository.findCartItemsWithFilters(
                userId, productId, userEmail, userName, productName,
                minQuantity, maxQuantity, fromDate, toDate, pageable);
        
        List<AdminCartItemDto> items = cartItemPage.getContent().stream()
                .map(this::mapToAdminCartItemDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return PageResponseDto.<AdminCartItemDto>builder()
                .content(items)
                .page(cartItemPage.getNumber())
                .size(cartItemPage.getSize())
                .totalElements(cartItemPage.getTotalElements())
                .totalPages(cartItemPage.getTotalPages())
                .last(cartItemPage.isLast())
                .build();
    }

    /**
     * Get detailed cart for a specific user
     */
    public AdminCartSummaryDto getUserCartDetails(Long userId) { // Renamed to match controller
        
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        // Get user's cart
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new NotFoundException("Cart not found for user: " + userId));
        
        List<AdminCartItemDto> items = cart.getItems().stream()
                .map(this::mapToAdminCartItemDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (items.isEmpty()) {
            throw new NotFoundException("No valid cart items found for user: " + userId);
        }
        
        Integer totalQuantity = items.stream()
                .mapToInt(AdminCartItemDto::getQuantity)
                .sum();
        
        Double totalValue = items.stream()
                .mapToDouble(AdminCartItemDto::getSubtotal)
                .sum();
        
        Instant lastActivity = cart.getUpdatedAt() != null ? 
                cart.getUpdatedAt() : cart.getCreatedAt();
        
        return AdminCartSummaryDto.builder()
                .userId(userId)
                .userName(user.getName() != null ? user.getName() : "N/A")
                .userEmail(user.getEmail() != null ? user.getEmail() : "N/A")
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .totalValue(totalValue)
                .lastActivity(lastActivity)
                .items(items)
                .build();
    }

    /**
     * Get all carts grouped by user (summary view)
     */
    public List<AdminCartSummaryDto> getCartSummaries() {
        
        List<Cart> allCarts = cartRepository.findAllActiveCarts();
        
        if (allCarts == null || allCarts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group by user
        Map<Long, List<CartItem>> itemsByUser = allCarts.stream()
                .filter(cart -> cart != null && cart.getUser() != null)
                .collect(Collectors.toMap(
                        cart -> cart.getUser().getId(),
                        cart -> new ArrayList<>(cart.getItems()),
                        (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        }
                ));
        
        List<AdminCartSummaryDto> summaries = new ArrayList<>();
        
        for (Map.Entry<Long, List<CartItem>> entry : itemsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<CartItem> items = entry.getValue();
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;
            
            List<AdminCartItemDto> itemDtos = items.stream()
                    .map(this::mapToAdminCartItemDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (itemDtos.isEmpty()) continue;
            
            summaries.add(AdminCartSummaryDto.builder()
                    .userId(userId)
                    .userName(user.getName() != null ? user.getName() : "N/A")
                    .userEmail(user.getEmail() != null ? user.getEmail() : "N/A")
                    .totalItems(itemDtos.size())
                    .totalQuantity(itemDtos.stream().mapToInt(AdminCartItemDto::getQuantity).sum())
                    .totalValue(itemDtos.stream().mapToDouble(AdminCartItemDto::getSubtotal).sum())
                    .lastActivity(Instant.now()) // You might want to track this
                    .items(itemDtos)
                    .build());
        }
        
        return summaries.stream()
                .sorted(Comparator.comparing(AdminCartSummaryDto::getTotalValue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Helper: Map CartItem to AdminCartItemDto
     */
    private AdminCartItemDto mapToAdminCartItemDto(CartItem cartItem) {
        
        if (cartItem == null) {
            return null;
        }
        
        Product product = cartItem.getVariant() != null ? 
                cartItem.getVariant().getProduct() : null;
        
        if (product == null) {
            log.warn("Cart item {} has null product", cartItem.getId());
            return null;
        }
        
        User user = cartItem.getCart() != null ? 
                cartItem.getCart().getUser() : null;
        
        Double price = cartItem.getVariant().getSellingPrice() != null ? 
                cartItem.getVariant().getSellingPrice().doubleValue() : 0.0;
        Integer quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
        Double subtotal = price * quantity;
        
        // Get available stock
        Integer availableStock = cartItem.getVariant().getInventories() != null ?
                cartItem.getVariant().getInventories().stream()
                        .mapToInt(wi -> wi.getAvailableQuantity() != null ? wi.getAvailableQuantity() : 0)
                        .sum() : 0;
        
        Boolean inStock = availableStock >= quantity;
        
        return AdminCartItemDto.builder()
                .cartId(cartItem.getCart().getId())
                .itemId(cartItem.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : "N/A")
                .userEmail(user != null ? user.getEmail() : "N/A")
                .productId(product.getId())
                .productName(product.getName() != null ? product.getName() : "Unknown Product")
                .productImage(getPrimaryImage(product))
                .productPrice(price)
                .quantity(quantity)
                .subtotal(subtotal)
                .inStock(inStock)
               
                .build();
    }

    /**
     * Helper: Get primary product image
     */
    private String getPrimaryImage(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        
        return product.getImages().stream()
                .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(product.getImages().get(0).getImageUrl());
    }

    /**
     * Get abandoned carts (older than 48 hours)
     */
    public List<AdminCartSummaryDto> getAbandonedCarts() {
        
        Instant threshold = Instant.now().minus(48, ChronoUnit.HOURS);
        List<Cart> oldCarts = cartRepository.findCartsUpdatedBefore(threshold);
        
        if (oldCarts == null || oldCarts.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AdminCartSummaryDto> abandonedCarts = new ArrayList<>();
        
        for (Cart cart : oldCarts) {
            if (cart == null || cart.getUser() == null || cart.getItems().isEmpty()) {
                continue;
            }
            
            List<AdminCartItemDto> items = cart.getItems().stream()
                    .map(this::mapToAdminCartItemDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (items.isEmpty()) continue;
            
            abandonedCarts.add(AdminCartSummaryDto.builder()
                    .userId(cart.getUser().getId())
                    .userName(cart.getUser().getName() != null ? cart.getUser().getName() : "N/A")
                    .userEmail(cart.getUser().getEmail() != null ? cart.getUser().getEmail() : "N/A")
                    .totalItems(items.size())
                    .totalQuantity(items.stream().mapToInt(AdminCartItemDto::getQuantity).sum())
                    .totalValue(items.stream().mapToDouble(AdminCartItemDto::getSubtotal).sum())
                    .lastActivity(cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt())
                    .items(items)
                    .build());
        }
        
        return abandonedCarts;
    }

    /**
     * Admin action: Remove specific cart item
     */
    @Transactional
    public void removeCartItem(Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + itemId));
        
        log.info("Admin removed cart item {} for user {}", itemId, 
                cartItem.getCart().getUser().getId());
        cartItemRepository.delete(cartItem);
    }

    /**
     * Admin action: Clear entire user cart
     */
    @Transactional
    public void clearUserCart(Long userId) {
        
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found with ID: " + userId);
        }
        
//        Cart cart = cartRepository.findByUserId(userId)
//                .orElseThrow(() -> new NotFoundException("Cart not found for user: " + userId));
//        
//        log.info("Admin cleared cart for user {}. Removed {} items", 
//                userId, cart.getItems().size());
//        
//        cartItemRepository.deleteByCartId(cart.getId());
    }
    
    /**
     * Admin action: Update cart item quantity
     */
    @Transactional
    public AdminCartItemDto updateCartItemQuantity(Long itemId, Integer newQuantity) {
        
        if (newQuantity == null || newQuantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + itemId));
        
        // Check stock
        Integer availableStock = cartItem.getVariant().getInventories().stream()
                .mapToInt(wi -> wi.getAvailableQuantity() != null ? wi.getAvailableQuantity() : 0)
                .sum();
        
        if (newQuantity > availableStock) {
            throw new BadRequestException("Only " + availableStock + " units available");
        }
        
        cartItem.setQuantity(newQuantity);
        CartItem updated = cartItemRepository.save(cartItem);
        
        log.info("Admin updated cart item {} quantity to {}", itemId, newQuantity);
        
        return mapToAdminCartItemDto(updated);
    }
    
    /**
     * Get cart statistics for dashboard
     */
    public CartStatisticsDto getCartStatistics() {
        
        Long totalUsersWithCart = cartRepository.countUsersWithActiveCart();
        Long totalItems = cartItemRepository.countTotalItems();
      //  Double totalValue = cartItemRepository.sumTotalCartValue();
        
        // Abandoned carts (older than 48 hours)
        Instant threshold = Instant.now().minus(48, ChronoUnit.HOURS);
        Long abandonedCount = cartRepository.countAbandonedCarts(threshold);
        Double abandonedValue = cartItemRepository.sumAbandonedCartValue(threshold);
        
        // Get top products in carts
        List<Map<String, Object>> topProducts = getTopProductsInCarts(5);
        
        return CartStatisticsDto.builder()
                .totalUsersWithCart(totalUsersWithCart != null ? totalUsersWithCart : 0L)
                .totalCartItems(totalItems != null ? totalItems : 0L)
              //  .totalCartValue(totalValue != null ? totalValue : 0.0)
            //    .averageCartValue(totalUsersWithCart != null && totalUsersWithCart > 0 )
                .abandonedCartsCount(abandonedCount != null ? abandonedCount : 0L)
                .abandonedCartsValue(abandonedValue != null ? abandonedValue : 0.0)
                .topProducts(topProducts != null ? topProducts : new ArrayList<>())
                .build();
    }
    
    /**
     * Helper: Get top products in carts
     */
    private List<Map<String, Object>> getTopProductsInCarts(int limit) {
        
        List<Object[]> results = cartItemRepository.findTopProducts(limit);
        
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", row[0]);
                    map.put("productName", row[1]);
                    map.put("totalQuantity", row[2]);
                    map.put("totalValue", row[3]);
                    map.put("userCount", row[4]);
                    return map;
                })
                .collect(Collectors.toList());
    }
}