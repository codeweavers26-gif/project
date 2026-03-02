package com.project.backend.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final CartItemRepository cartItemRepository; 
    private final UserRepository userRepository;

    public PageResponseDto<AdminCartItemDto> getCartItems(
            Long userId, Long productId, String userEmail, String userName,
            String productName, Integer minQuantity, Integer maxQuantity,
            Instant fromDate, Instant toDate, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("addedAt").descending());
        
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

    public AdminCartSummaryDto getUserCartDetails(Long userId) { 
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
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
    private String calculateCartAge(Instant lastActivity) {
        if (lastActivity == null) return "Unknown";
        
        long hours = ChronoUnit.HOURS.between(lastActivity, Instant.now());
        long days = ChronoUnit.DAYS.between(lastActivity, Instant.now());
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            long minutes = ChronoUnit.MINUTES.between(lastActivity, Instant.now());
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
    }

    public PageResponseDto<AdminCartSummaryDto> getCartSummaries(int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalValue").descending());
        
        Page<Object[]> summaryPage = cartRepository.findCartSummaries(pageable);
        
        if (summaryPage.isEmpty()) {
            return PageResponseDto.<AdminCartSummaryDto>builder()
                    .content(new ArrayList<>())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }
        
        List<AdminCartSummaryDto> summaries = summaryPage.getContent().stream()
                .map(this::mapToAdminCartSummaryFromRow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return PageResponseDto.<AdminCartSummaryDto>builder()
                .content(summaries)
                .page(summaryPage.getNumber())
                .size(summaryPage.getSize())
                .totalElements(summaryPage.getTotalElements())
                .totalPages(summaryPage.getTotalPages())
                .last(summaryPage.isLast())
                .build();
    }
    
    private AdminCartSummaryDto mapToAdminCartSummaryFromRow(Object[] row) {
        try {
            Long userId = (Long) row[0];
            String userName = (String) row[1];
            String userEmail = (String) row[2];
            Long totalItems = ((Number) row[3]).longValue();
            Integer totalQuantity = ((Number) row[4]).intValue();
            Double totalValue = ((Number) row[5]).doubleValue();
            Instant lastActivity = (Instant) row[6];
            String cartAge = calculateCartAge(lastActivity);

            return AdminCartSummaryDto.builder()
                    .userId(userId)
                    .userName(userName != null ? userName : "N/A")
                    .userEmail(userEmail != null ? userEmail : "N/A")
                    .totalItems((int) totalItems.longValue())
                    .totalQuantity(totalQuantity)
                    .totalValue(Math.round(totalValue * 100.0) / 100.0)
                    .lastActivity(lastActivity)
                   .cartAge(cartAge)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error mapping cart summary row: {}", e.getMessage());
            return null;
        }
    }

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
        Instant addedAt = null;
        if (cartItem.getAddedAt() != null) {
            addedAt = cartItem.getAddedAt().atZone(ZoneId.systemDefault()).toInstant();
        }
        
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
               .addedAt(addedAt)
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


    public List<AdminCartSummaryDto> getAbandonedCarts() {
        
        Instant threshold = Instant.now().minus(48, ChronoUnit.HOURS);
        List<Cart> oldCarts = cartRepository.findAbandonedCartsWithItems(threshold);
        
        if (oldCarts == null || oldCarts.isEmpty()) {
            return new ArrayList<>();
        }
        
        return oldCarts.stream()
            .filter(cart -> cart != null && cart.getUser() != null && cart.getItems() != null && !cart.getItems().isEmpty())
            .map(this::mapToAbandonedCartSummary)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private AdminCartSummaryDto mapToAbandonedCartSummary(Cart cart) {
        try {
            List<AdminCartItemDto> items = cart.getItems().stream()
                .map(this::mapToAdminCartItemDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (items.isEmpty()) {
                return null;
            }
            
            int totalQuantity = items.stream()
                .mapToInt(AdminCartItemDto::getQuantity)
                .sum();
            
            double totalValue = items.stream()
                .mapToDouble(AdminCartItemDto::getSubtotal)
                .sum();
            
            return AdminCartSummaryDto.builder()
                .userId(cart.getUser().getId())
                .userName(cart.getUser().getName() != null ? cart.getUser().getName() : "N/A")
                .userEmail(cart.getUser().getEmail() != null ? cart.getUser().getEmail() : "N/A")
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .totalValue(Math.round(totalValue * 100.0) / 100.0) 
                .lastActivity(cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt())
                .cartAge(getCartAge(cart))
                .items(items)
                .build();
                
        } catch (Exception e) {
            log.error("Error mapping abandoned cart: {}", e.getMessage());
            return null;
        }
    }

    private String getCartAge(Cart cart) {
        Instant lastActivity = cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt();
        long hours = ChronoUnit.HOURS.between(lastActivity, Instant.now());
        
        if (hours < 24) {
            return hours + " hours";
        } else {
            long days = hours / 24;
            return days + " days";
        }
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

    @Transactional
    public AdminCartItemDto updateCartItemQuantity(Long itemId, Integer newQuantity) {
        
        if (newQuantity == null || newQuantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + itemId));
        
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

    public CartStatisticsDto getCartStatistics() {
        log.info("Fetching cart statistics for dashboard");
        
        Long totalUsersWithCart = cartRepository.countUsersWithActiveCart();
        Long totalItems = cartItemRepository.countTotalItems();
        Double totalValue = cartRepository.getTotalCartValue();
        Long totalCarts = cartRepository.count();
        
        Instant threshold = Instant.now().minus(48, ChronoUnit.HOURS);
        Long abandonedCount = cartRepository.countAbandonedCarts(threshold);
        Double abandonedValue = cartItemRepository.sumAbandonedCartValue(threshold);
        
        Double averageCartValue = 0.0;
        if (totalUsersWithCart != null && totalUsersWithCart > 0 && totalValue != null) {
            averageCartValue = totalValue / totalUsersWithCart;
        }
        
        Double averageItemsPerCart = 0.0;
        Long cartsWithItems = cartRepository.countCartsWithItems();
        Long emptyCarts = cartRepository.countEmptyCarts();
        Double avgItemsPerCart = cartRepository.averageItemsPerCart();
        
        if (totalUsersWithCart != null && totalUsersWithCart > 0) {
            averageCartValue = totalValue / totalUsersWithCart;
        }
        
        if (totalCarts != null && totalCarts > 0) {

            averageItemsPerCart = totalItems / (double) totalCarts;
        }
        
        List<Map<String, Object>> topProducts = getTopProductsInCarts(5);
        
        return CartStatisticsDto.builder()
                .totalUsersWithCart(totalUsersWithCart != null ? totalUsersWithCart : 0L)
                .totalCartItems(totalItems != null ? totalItems : 0L)
                .totalCartValue(totalValue != null ? Math.round(totalValue * 100.0) / 100.0 : 0.0)
                .averageCartValue(Math.round(averageCartValue * 100.0) / 100.0)
                .averageItemsPerCart(avgItemsPerCart != null ? Math.round(avgItemsPerCart * 100.0) / 100.0 : 0.0)
                .abandonedCartsCount(abandonedCount != null ? abandonedCount : 0L)
                .abandonedCartsValue(abandonedValue != null ? Math.round(abandonedValue * 100.0) / 100.0 : 0.0)
                .topProducts(topProducts != null ? topProducts : new ArrayList<>())
                .totalCarts(totalCarts != null ? totalCarts : 0L)
                .cartsWithItems(cartsWithItems != null ? cartsWithItems : 0L)
                .emptyCarts(emptyCarts != null ? emptyCarts : 0L)
                .build();
    }

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