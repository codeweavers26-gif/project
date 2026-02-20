package com.project.backend.service;

import com.project.backend.ResponseDto.AdminCartSummaryDto;
import com.project.backend.ResponseDto.AdminCartItemDto;
import com.project.backend.ResponseDto.CartStatisticsDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.User;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminCartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    /**
     * Get paginated cart items with filters
     */
    public PageResponseDto<AdminCartItemDto> getCartItems(
            Long userId, Long productId, String userEmail, String userName,
            String productName, Integer minQuantity, Integer maxQuantity,
            Instant fromDate, Instant toDate, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Cart> cartPage = cartRepository.findCartsWithFilters(
                userId, productId, userEmail, userName, productName,
                minQuantity, maxQuantity, fromDate, toDate, pageable);
        
        List<AdminCartItemDto> items = cartPage.getContent().stream()
                .map(this::mapToAdminCartItemDto)
                .filter(Objects::nonNull) // Filter out null items
                .collect(Collectors.toList());
        
        return PageResponseDto.<AdminCartItemDto>builder()
                .content(items)
                .page(cartPage.getNumber())
                .size(cartPage.getSize())
                .totalElements(cartPage.getTotalElements())
                .totalPages(cartPage.getTotalPages())
                .last(cartPage.isLast())
                .build();
    }

    /**
     * Get detailed cart for a specific user
     */
    public AdminCartSummaryDto getUserCart(Long userId) {
        
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        
        // Get user's cart items
        List<Cart> userCart = cartRepository.findByUserId(userId);
        
        if (userCart == null || userCart.isEmpty()) {
            throw new NotFoundException("Cart is empty for user: " + userId);
        }
        
        List<AdminCartItemDto> items = userCart.stream()
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
        
        Instant lastActivity = userCart.stream()
                .map(Cart::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(Instant.now());
        
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
        
        List<Long> userIds = cartRepository.findDistinctUserIds();
        
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<AdminCartSummaryDto> summaries = new ArrayList<>();
        
        for (Long userId : userIds) {
            try {
                summaries.add(getUserCart(userId));
            } catch (Exception e) {
                log.warn("Error fetching cart for user {}: {}", userId, e.getMessage());
            }
        }
        
        return summaries.stream()
                .sorted(Comparator.comparing(AdminCartSummaryDto::getTotalValue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Helper: Map Cart to AdminCartItemDto with null safety
     */
    private AdminCartItemDto mapToAdminCartItemDto(Cart cart) {
        
        if (cart == null) {
            return null;
        }
        
        // Safely get product with null check
        Product product = cart.getProduct();
        if (product == null) {
            log.warn("Cart item {} has null product", cart.getId());
            return null;
        }
        
        // Safely get user with null check
        User user = cart.getUser();
        if (user == null) {
            log.warn("Cart item {} has null user", cart.getId());
            return null;
        }
        
        Double price = product.getPrice() != null ? product.getPrice() : 0.0;
        Integer quantity = cart.getQuantity() != null ? cart.getQuantity() : 0;
        Double subtotal = price * quantity;
        
        Integer stock = product.getStock() != null ? product.getStock() : 0;
        Boolean inStock = stock >= quantity;
        
        return AdminCartItemDto.builder()
                .cartId(cart.getId())
                .productId(product.getId())
                .productName(product.getName() != null ? product.getName() : "Unknown Product")
                .productImage(getPrimaryImage(product))
                .productPrice(price)
                .quantity(quantity)
                .subtotal(subtotal)
                .inStock(inStock)
                .addedAt(cart.getCreatedAt() != null ? cart.getCreatedAt() : Instant.now())
                .build();
    }

    /**
     * Helper: Get primary product image with null safety
     */
    private String getPrimaryImage(Product product) {
        if (product == null) {
            return null;
        }
        
        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            return null;
        }
        
        return images.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    if (a == null || b == null) return 0;
                    Integer posA = a.getPosition() != null ? a.getPosition() : 0;
                    Integer posB = b.getPosition() != null ? b.getPosition() : 0;
                    return posA.compareTo(posB);
                })
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    /**
     * Get abandoned carts (older than 24 hours)
     */
    public List<AdminCartSummaryDto> getAbandonedCarts() {
        
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Cart> oldCarts = cartRepository.findCartsOlderThan(threshold);
        
        if (oldCarts == null || oldCarts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group by user
        Map<Long, List<Cart>> cartsByUser = oldCarts.stream()
                .filter(cart -> cart != null && cart.getUser() != null)
                .collect(Collectors.groupingBy(cart -> cart.getUser().getId()));
        
        List<AdminCartSummaryDto> abandonedCarts = new ArrayList<>();
        
        for (Map.Entry<Long, List<Cart>> entry : cartsByUser.entrySet()) {
            Long userId = entry.getKey();
            List<Cart> userCarts = entry.getValue();
            
            if (userCarts == null || userCarts.isEmpty()) {
                continue;
            }
            
            User user = userCarts.get(0).getUser();
            if (user == null) {
                continue;
            }
            
            List<AdminCartItemDto> items = userCarts.stream()
                    .map(this::mapToAdminCartItemDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (items.isEmpty()) {
                continue;
            }
            
            Double totalValue = items.stream()
                    .mapToDouble(AdminCartItemDto::getSubtotal)
                    .sum();
            
            abandonedCarts.add(AdminCartSummaryDto.builder()
                    .userId(userId)
                    .userName(user.getName() != null ? user.getName() : "N/A")
                    .userEmail(user.getEmail() != null ? user.getEmail() : "N/A")
                    .totalItems(items.size())
                    .totalQuantity(items.stream().mapToInt(AdminCartItemDto::getQuantity).sum())
                    .totalValue(totalValue)
                    .lastActivity(userCarts.stream()
                            .map(Cart::getCreatedAt)
                            .filter(Objects::nonNull)
                            .max(Instant::compareTo).orElse(null))
                    .items(items)
                    .build());
        }
        
        return abandonedCarts;
    }

    /**
     * Admin action: Remove specific cart item
     */
    @Transactional
    public void removeCartItem(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + cartId));
        
        log.info("Admin removed cart item {} for user {}", cartId, 
                cart.getUser() != null ? cart.getUser().getId() : "unknown");
        cartRepository.delete(cart);
    }

    /**
     * Admin action: Clear entire user cart
     */
    @Transactional
    public void clearUserCart(Long userId) {
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found with ID: " + userId);
        }
        
        List<Cart> userCart = cartRepository.findByUserId(userId);
        
        if (userCart == null || userCart.isEmpty()) {
            throw new NotFoundException("Cart is empty for user: " + userId);
        }
        
        log.info("Admin cleared cart for user {}. Removed {} items", userId, userCart.size());
        cartRepository.deleteByUserId(userId);
    }
    
    /**
     * Admin action: Update cart item quantity
     */
    @Transactional
    public AdminCartItemDto updateCartItemQuantity(Long cartId, Integer newQuantity) {
        
        if (newQuantity == null || newQuantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + cartId));
        
        cart.setQuantity(newQuantity);
        Cart updated = cartRepository.save(cart);
        
        log.info("Admin updated cart item {} quantity to {}", cartId, newQuantity);
        
        return mapToAdminCartItemDto(updated);
    }
    /**
     * Get cart statistics for dashboard
     */
    public CartStatisticsDto getCartStatistics() {
        
        Long totalUsersWithCart = cartRepository.countUsersWithCart();
        Long totalItems = cartRepository.countTotalItems();
        Double totalValue = cartRepository.sumTotalCartValue();
        
        // Abandoned carts (older than 24 hours)
        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Cart> abandonedCarts = cartRepository.findCartsOlderThan(threshold);
        
        Set<Long> abandonedUserIds = abandonedCarts.stream()
                .filter(cart -> cart != null && cart.getUser() != null)
                .map(cart -> cart.getUser().getId())
                .collect(Collectors.toSet());
        
        Double abandonedValue = abandonedCarts.stream()
                .filter(cart -> cart != null && cart.getProduct() != null)
                .mapToDouble(cart -> {
                    Double price = cart.getProduct().getPrice() != null ? cart.getProduct().getPrice() : 0.0;
                    Integer qty = cart.getQuantity() != null ? cart.getQuantity() : 0;
                    return price * qty;
                })
                .sum();
        
        // Get top products in carts
        List<Map<String, Object>> topProducts = getTopProductsInCarts(5);
        
        return CartStatisticsDto.builder()
                .totalUsersWithCart(totalUsersWithCart != null ? totalUsersWithCart : 0L)
                .totalCartItems(totalItems != null ? totalItems : 0L)
                .totalCartValue(totalValue != null ? totalValue : 0.0)
                .averageCartValue(totalUsersWithCart != null && totalUsersWithCart > 0 
                        ? (totalValue != null ? totalValue / totalUsersWithCart : 0) : 0)
                .abandonedCartsCount(abandonedUserIds != null ? (long) abandonedUserIds.size() : 0L)
                .abandonedCartsValue(abandonedValue)
                .topProducts(topProducts != null ? topProducts : new ArrayList<>())
                .build();
    }
    
    /**
     * Helper: Get top products in carts
     */
    private List<Map<String, Object>> getTopProductsInCarts(int limit) {
        
        List<Cart> allCarts = cartRepository.findAll();
        
        if (allCarts == null || allCarts.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<Long, Map<String, Object>> productMap = new HashMap<>();
        
        for (Cart cart : allCarts) {
            if (cart == null || cart.getProduct() == null) {
                continue;
            }
            
            Product product = cart.getProduct();
            Long productId = product.getId();
            Integer quantity = cart.getQuantity() != null ? cart.getQuantity() : 0;
            Double price = product.getPrice() != null ? product.getPrice() : 0.0;
            
            if (!productMap.containsKey(productId)) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("productId", productId);
                productData.put("productName", product.getName() != null ? product.getName() : "Unknown");
                productData.put("totalQuantity", 0);
                productData.put("totalValue", 0.0);
                productData.put("userCount", 0);
                productMap.put(productId, productData);
            }
            
            Map<String, Object> data = productMap.get(productId);
            data.put("totalQuantity", (Integer) data.get("totalQuantity") + quantity);
            data.put("totalValue", (Double) data.get("totalValue") + (price * quantity));
            data.put("userCount", (Integer) data.get("userCount") + 1);
        }
        
        return productMap.values().stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    Double valueA = (Double) a.get("totalValue");
                    Double valueB = (Double) b.get("totalValue");
                    return valueB.compareTo(valueA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}