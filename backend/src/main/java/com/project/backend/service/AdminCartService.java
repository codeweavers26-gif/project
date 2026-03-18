package com.project.backend.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.project.backend.entity.CartItem;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CartItemRepository;
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
    private static final int ABANDONED_CART_HOURS = 48;
    private static final String DEFAULT_VALUE = "N/A";
    private static final int HOURS_PER_DAY = 24;
    private static final String UNKNOWN_AGE = "Unknown";

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
                .toList();

        return buildPageResponse(cartItemPage, items);
    }

    private <T> PageResponseDto<T> buildPageResponse(Page<?> page, List<T> content) {
        return PageResponseDto.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
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

        Instant lastActivity = cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt();

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
        if (lastActivity == null)
            return UNKNOWN_AGE;

        long hours = ChronoUnit.HOURS.between(lastActivity, Instant.now());
        long days = hours / HOURS_PER_DAY;

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

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "totalValue"));

        Page<Object[]> summaryPage = cartRepository.findCartSummaries(pageable);

        if (summaryPage.isEmpty()) {
            return PageResponseDto.<AdminCartSummaryDto>builder()
                    .content(List.of())
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
                .toList();

        return buildPageResponse(summaryPage, summaries);
    }

    private static final double ROUNDING_FACTOR = 100.0;

    private AdminCartSummaryDto mapToAdminCartSummaryFromRow(Object[] row) {
        try {
            if (row == null || row.length < 7) {
                log.warn("Invalid row data for cart summary");
                return null;
            }

            Long userId = safeCastToLong(row[0]);
            if (userId == null)
                return null;

            String userName = safeCastToString(row[1]);
            String userEmail = safeCastToString(row[2]);
            Long totalItems = safeCastToLong(row[3]);
            Integer totalQuantity = safeCastToInteger(row[4]);
            Double totalValue = safeCastToDouble(row[5]);
            Instant lastActivity = safeCastToInstant(row[6]);

            return AdminCartSummaryDto.builder()
                    .userId(userId)
                    .userName(userName != null ? userName : DEFAULT_VALUE)
                    .userEmail(userEmail != null ? userEmail : DEFAULT_VALUE)
                    .totalItems(totalItems != null ? totalItems.intValue() : 0)
                    .totalQuantity(totalQuantity != null ? totalQuantity : 0)
                    .totalValue(totalValue != null ? roundToTwoDecimals(totalValue) : 0.0)
                    .lastActivity(lastActivity)
                    .cartAge(calculateCartAge(lastActivity))
                    .build();

        } catch (Exception e) {
            log.error("Error mapping cart summary row: {}", e);
            return null;
        }
    }

    private Long safeCastToLong(Object obj) {
        return obj instanceof Number ? ((Number) obj).longValue() : null;
    }

    private String safeCastToString(Object obj) {
        return obj instanceof String ? (String) obj : null;
    }

    private Integer safeCastToInteger(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : null;
    }

    private Double safeCastToDouble(Object obj) {
        return obj instanceof Number ? ((Number) obj).doubleValue() : null;
    }

    private Instant safeCastToInstant(Object obj) {
        return obj instanceof Instant ? (Instant) obj : null;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }

    private AdminCartItemDto mapToAdminCartItemDto(CartItem cartItem) {

        if (cartItem == null || cartItem.getVariant() == null) {
            return null;
        }

        Product product = cartItem.getVariant().getProduct();
        if (product == null) {
            log.warn("Product missing for cartItemId={}", cartItem.getId());
            return null;
        }

        User user = Optional.ofNullable(cartItem.getCart())
                .map(Cart::getUser)
                .orElse(null);

        double price = Optional.ofNullable(cartItem.getVariant().getSellingPrice())
                .map(Number::doubleValue)
                .orElse(0.0);

        int quantity = Optional.ofNullable(cartItem.getQuantity()).orElse(0);
        double subtotal = price * quantity;

        boolean inStock = checkStockAvailability(cartItem, quantity);

        return AdminCartItemDto.builder()
                .cartId(cartItem.getCart().getId())
                .itemId(cartItem.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : DEFAULT_VALUE)
                .userEmail(user != null ? user.getEmail() : DEFAULT_VALUE)
                .productId(product.getId())
                .productName(Optional.ofNullable(product.getName()).orElse("Unknown Product"))
                .productImage(getPrimaryImage(product))
                .productPrice(price)
                .quantity(quantity)
                .subtotal(subtotal)
                .inStock(inStock)
                .addedAt(convertToInstant(cartItem.getAddedAt()))
                .build();
    }

    private boolean checkStockAvailability(CartItem cartItem, int requestedQuantity) {
        int availableStock = Optional.ofNullable(cartItem.getVariant().getInventories())
                .orElse(List.of())
                .stream()
                .mapToInt(inv -> Optional.ofNullable(inv.getAvailableQuantity()).orElse(0))
                .sum();

        return availableStock >= requestedQuantity;
    }

    private Instant convertToInstant(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).toInstant()
                : null;
    }

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

        Instant threshold = Instant.now().minus(ABANDONED_CART_HOURS, ChronoUnit.HOURS);
        List<Cart> oldCarts = cartRepository.findAbandonedCartsWithItems(threshold);

        if (oldCarts == null || oldCarts.isEmpty()) {
            return List.of();
        }

        return oldCarts.stream()
                .filter(cart -> cart != null && cart.getUser() != null &&
                        cart.getItems() != null && !cart.getItems().isEmpty())
                .map(this::mapToAbandonedCartSummary)
                .filter(Objects::nonNull)
                .toList();
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
Instant lastActivity = cart.getUpdatedAt() != null ? cart.getUpdatedAt() : cart.getCreatedAt();
            return AdminCartSummaryDto.builder()
                    .userId(cart.getUser().getId())
                    .userName(cart.getUser().getName() != null ? cart.getUser().getName() : "N/A")
                    .userEmail(cart.getUser().getEmail() != null ? cart.getUser().getEmail() : "N/A")
                    .totalItems(items.size())
                    .totalQuantity(totalQuantity)
                    .totalValue(Math.round(totalValue * 100.0) / 100.0)
                    .lastActivity(lastActivity)
                    .cartAge(calculateCartAge(lastActivity))
                    .items(items)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping abandoned cart: {}", e);
            return null;
        }
    }

    @Transactional
    public void removeCartItem(Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + itemId));

        Long userId = cartItem.getCart().getUser().getId();
        cartItemRepository.delete(cartItem);
        log.info("Admin removed cart item {} for user {}", itemId, userId);
    }

    @Transactional
    public AdminCartItemDto updateCartItemQuantity(Long itemId, Integer newQuantity) {

        validateQuantity(newQuantity);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with ID: " + itemId));

        validateStockForQuantity(cartItem, newQuantity);

        cartItem.setQuantity(newQuantity);
        CartItem updated = cartItemRepository.save(cartItem);

        log.info("Admin updated cart item {} quantity to {}", itemId, newQuantity);

        return mapToAdminCartItemDto(updated);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
    }

    private void validateStockForQuantity(CartItem cartItem, int requestedQuantity) {
        if (!checkStockAvailability(cartItem, requestedQuantity)) {
            int availableStock = getAvailableStock(cartItem);
            throw new BadRequestException("Only " + availableStock + " units available");
        }
    }

    private int getAvailableStock(CartItem cartItem) {
        return Optional.ofNullable(cartItem.getVariant().getInventories())
                .orElse(List.of())
                .stream()
                .mapToInt(inv -> Optional.ofNullable(inv.getAvailableQuantity()).orElse(0))
                .sum();
    }

    public CartStatisticsDto getCartStatistics() {
        log.info("Fetching cart statistics for dashboard");

        Long totalUsersWithCart = cartRepository.countUsersWithActiveCart();
        Long totalItems = cartItemRepository.countTotalItems();
        Double totalValue = cartRepository.getTotalCartValue();
        Long totalCarts = cartRepository.count();
Instant threshold = Instant.now().minus(ABANDONED_CART_HOURS, ChronoUnit.HOURS);
        Long abandonedCount = cartRepository.countAbandonedCarts(threshold);
        Double abandonedValue = cartItemRepository.sumAbandonedCartValue(threshold);

       

        Double averageItemsPerCart = 0.0;
        Long cartsWithItems = cartRepository.countCartsWithItems();
        Long emptyCarts = cartRepository.countEmptyCarts();
        Double avgItemsPerCart = cartRepository.averageItemsPerCart();

      
double averageCartValue = (totalUsersWithCart != null && totalUsersWithCart > 0 && totalValue != null)
        ? totalValue / totalUsersWithCart
        : 0.0;
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
        return List.of();
    }
    
    return results.stream()
            .map(this::mapToProductStat)
            .toList();
}

private Map<String, Object> mapToProductStat(Object[] row) {
    Map<String, Object> map = new HashMap<>(5);
    map.put("productId", row[0]);
    map.put("productName", row[1]);
    map.put("totalQuantity", row[2]);
    map.put("totalValue", row[3]);
    map.put("userCount", row[4]);
    return map;
}
}