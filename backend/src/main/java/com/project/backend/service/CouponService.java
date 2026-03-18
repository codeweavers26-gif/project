package com.project.backend.service;

import com.project.backend.entity.*;
import com.project.backend.exception.*;
import com.project.backend.repository.*;
import com.project.backend.requestDto.ApplyCouponRequest;
import com.project.backend.requestDto.BulkCouponCreateRequest;
import com.project.backend.requestDto.CouponDto;
import com.project.backend.requestDto.CouponStatsDto;
import com.project.backend.requestDto.CouponUsageDto;
import com.project.backend.requestDto.CouponValidationResult;
import com.project.backend.requestDto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final com.project.backend.config.CouponCodeGenerator codeGenerator;


private static final int DEFAULT_PAGE_SIZE = 20;
private static final int MAX_BULK_CREATE_LIMIT = 100;
private static final int NEW_USER_DAYS = 30;
private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
private static final String COUPON_ALREADY_USED = "Cannot edit coupon that has already been used";


@Transactional(readOnly = true)
public CouponDto getCouponById(Long id) {
    log.info("Fetching coupon by ID: {}", id);
    
    return couponRepository.findById(id)
        .map(this::mapToDto)
        .orElseThrow(() -> new NotFoundException("Coupon not found with ID: " + id));
}



@Transactional(readOnly = true)
public PageResponseDto<CouponUsageDto> getCouponUsage(Long couponId, Pageable pageable) {
    log.info("Fetching usage history for coupon ID: {}", couponId);
    
    if (!couponRepository.existsById(couponId)) {
        throw new NotFoundException("Coupon not found with ID: " + couponId);
    }
    
    Page<CouponUsage> usages = usageRepository.findByCouponId(couponId, pageable);
    
    return buildPageResponse(usages, this::mapToUsageDto);
}




private <T, R> PageResponseDto<R> buildPageResponse(Page<T> page, java.util.function.Function<T, R> mapper) {
    List<R> content = page.getContent().stream()
            .map(mapper)
            .toList();
    
    return PageResponseDto.<R>builder()
            .content(content)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
}


   @Transactional(readOnly = true)
    public PageResponseDto<CouponDto> getAllCoupons(
            String search,
            CouponType type,
            CouponStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        
        log.info("Fetching coupons with filters - search: {}, type: {}, status: {}", 
                 search, type, status);
        
    
            Page<Coupon> coupons = couponRepository.findByFilters(

                search, type, status, fromDate, toDate, pageable);
            
            return buildPageResponse(coupons, this::mapToDto);
                    
       
    }
    @Transactional(readOnly = true)
    public CouponDto getCouponByCode(String code) {
        log.info("Fetching coupon by code: {}", code);
        
            Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Coupon not found with code: " + code));
            
            return mapToDto(coupon);
            
     
    }

    @Transactional
    public CouponDto createCoupon(CouponDto couponDto, Long adminId) {
        log.info("Creating new coupon: {} by admin: {}", couponDto.getCode(), adminId);

        try {
            if (couponRepository.existsByCode(couponDto.getCode())) {
                throw new DuplicateResourceException("Coupon code already exists: " + couponDto.getCode());
            }

            if (couponDto.getValidFrom().isAfter(couponDto.getValidTo())) {
                throw new BadRequestException("Valid from date must be before valid to date");
            }

            if (couponDto.getValidTo().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Valid to date cannot be in the past");
            }

            validateDiscountValue(couponDto);

            Coupon coupon = mapToEntity(couponDto);
            coupon.setCreatedBy(adminId);
            coupon.setUpdatedBy(adminId);
            coupon.setStatus(couponDto.getValidFrom().isAfter(LocalDateTime.now()) ? 
                            CouponStatus.SCHEDULED : CouponStatus.ACTIVE);
            coupon.setTotalUsedCount(0);

            coupon = couponRepository.save(coupon);

            log.info("Coupon created successfully: {}", coupon.getCode());
            return mapToDto(coupon);

        } catch (DuplicateResourceException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating coupon", e);
            throw new BusinessException("Failed to create coupon", "COUPON_CREATE_ERROR");
        }
    }

    @Transactional
    public List<CouponDto> bulkCreateCoupons(BulkCouponCreateRequest request, Long adminId) {
        log.info("Bulk creating {} coupons with prefix: {}", request.getCount(), request.getPrefix());

        List<CouponDto> createdCoupons = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (int i = 0; i < request.getCount(); i++) {
            try {
                String uniqueCode = codeGenerator.generateUniqueCode(
                    request.getPrefix(), 
                    couponRepository::existsByCode
                );

                CouponDto newCoupon = request.getTemplate();
                newCoupon.setCode(uniqueCode);
                
                CouponDto created = createCoupon(newCoupon, adminId);
                createdCoupons.add(created);
                successCount++;

            } catch (Exception e) {
                failedCount++;
                log.warn("Failed to create coupon in bulk: {}", e.getMessage());
            }
        }

        log.info("Bulk creation completed: {} success, {} failed", successCount, failedCount);
        
        if (failedCount > 0 && successCount == 0) {
            throw new BusinessException("Failed to create any coupons", "BULK_CREATE_FAILED");
        }

        return createdCoupons;
    }
@Transactional
public CouponDto updateCoupon(Long id, CouponDto couponDto, Long adminId) {
    log.info("Updating coupon ID: {} by admin: {}", id, adminId);

    Coupon coupon = couponRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Coupon not found with ID: " + id));

    if (coupon.getTotalUsedCount() > 0) {
        throw new BadRequestException(COUPON_ALREADY_USED);
    }

    validateDates(couponDto.getValidFrom(), couponDto.getValidTo());
    validateDiscountValue(couponDto);

    updateEntity(coupon, couponDto);
    coupon.setUpdatedBy(adminId);
    updateCouponStatus(coupon);

    coupon = couponRepository.save(coupon);

    log.info("Coupon updated successfully: {}", coupon.getCode());
    return mapToDto(coupon);
}


private void validateDates(LocalDateTime from, LocalDateTime to) {
    if (from.isAfter(to)) {
        throw new BadRequestException("Valid from date must be before valid to date");
    }
    if (to.isBefore(LocalDateTime.now())) {
        throw new BadRequestException("Valid to date cannot be in the past");
    }
}

    @Transactional
    public void deleteCoupon(Long id) {
        log.info("Deleting coupon ID: {}", id);

            Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found with ID: " + id));

            Long usageCount = usageRepository.countByCouponId(id);
            if (usageCount > 0) {
                throw new BadRequestException("Cannot delete coupon that has been used. Disable it instead.");
            }

            couponRepository.delete(coupon);
            log.info("Coupon deleted successfully: {}", coupon.getCode());

       
    }

    @Transactional
    public CouponDto changeCouponStatus(Long id, CouponStatus newStatus, Long adminId) {
        log.info("Changing coupon {} status to: {} by admin: {}", id, newStatus, adminId);

            Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found with ID: " + id));

            coupon.setStatus(newStatus);
            coupon.setUpdatedBy(adminId);
            
            coupon = couponRepository.save(coupon);

         
            log.info("Coupon status changed to: {}", newStatus);
            return mapToDto(coupon);

       
    }

    @Transactional(readOnly = true)
    public CouponValidationResult validateAndApplyCoupon(ApplyCouponRequest request) {
        log.info("Validating coupon: {} for user: {}", request.getCouponCode(), request.getUserId());

            Coupon coupon = couponRepository.findByCode(request.getCouponCode().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Invalid coupon code"));

            List<String> warnings = new ArrayList<>();

            validateCouponBasicEligibility(coupon, request, warnings);

            if (request.getUserId() != null) {
                validateUserEligibility(coupon, request.getUserId(), request, warnings);
            }

            validateOrderEligibility(coupon, request, warnings);

            validateProductEligibility(coupon, request, warnings);

            BigDecimal discountAmount = calculateDiscount(coupon, request.getOrderAmount());

            if (coupon.getMaxDiscountAmount() != null && 
                discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discountAmount = coupon.getMaxDiscountAmount();
                warnings.add("Discount capped at maximum allowed amount");
            }

            BigDecimal finalAmount = request.getOrderAmount().subtract(discountAmount);

            return CouponValidationResult.builder()
                .valid(warnings.isEmpty())
                .message(warnings.isEmpty() ? "Coupon applied successfully" : 
                         "Coupon applied with warnings: " + String.join(", ", warnings))
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .coupon(mapToDto(coupon))
                .warnings(warnings)
                .build();

     
    }

    @Transactional
    public CouponUsage applyCouponToOrder(ApplyCouponRequest request, Long orderId) {
        log.info("Applying coupon {} to order: {}", request.getCouponCode(), orderId);

            CouponValidationResult validation = validateAndApplyCoupon(request);
            if (!validation.isValid()) {
                throw new BadRequestException(validation.getMessage());
            }

            Coupon coupon = couponRepository.findByCode(request.getCouponCode().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Coupon not found"));

            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

            if (usageRepository.existsByCouponIdAndUserIdAndOrderId(
                coupon.getId(), user.getId(), orderId)) {
                throw new BadRequestException("Coupon already applied to this order");
            }

            CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .discountAmount(validation.getDiscountAmount())
                
                .wasSuccessful(true)
                .build();

            usage = usageRepository.save(usage);

            coupon.setTotalUsedCount(coupon.getTotalUsedCount() + 1);
            couponRepository.save(coupon);

            order.setDiscountAmount(validation.getDiscountAmount().doubleValue());
            order.setTotalAmount(validation.getFinalAmount().doubleValue());
            orderRepository.save(order);

            log.info("Coupon applied successfully to order: {}", orderId);
            return usage;

    }

    @Transactional
    public void removeCouponFromOrder(Long orderId) {
        log.info("Removing coupon from order: {}", orderId);

            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

            Optional<CouponUsage> usage = usageRepository.findByOrderId(orderId);
            if (usage.isPresent()) {
                Coupon coupon = usage.get().getCoupon();
                coupon.setTotalUsedCount(coupon.getTotalUsedCount() - 1);
                couponRepository.save(coupon);
                
                usageRepository.delete(usage.get());
            }

            order.setDiscountAmount(0.0);
            order.setTotalAmount(order.getTotalAmount() + order.getTaxAmount() + 
                               order.getShippingCharges() - order.getDiscountAmount());
            orderRepository.save(order);

            log.info("Coupon removed from order: {}", orderId);

    }


    private void validateCouponBasicEligibility(Coupon coupon, ApplyCouponRequest request, List<String> warnings) {
        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BadRequestException("Coupon is not active. Status: " + coupon.getStatus());
        }

        if (now.isBefore(coupon.getValidFrom())) {
            throw new BadRequestException("Coupon is not yet valid. Valid from: " + coupon.getValidFrom());
        }

        if (now.isAfter(coupon.getValidTo())) {
            throw new BadRequestException("Coupon has expired. Expired on: " + coupon.getValidTo());
        }

        if (coupon.getUsageLimit() != null && 
            coupon.getTotalUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached");
        }

        if (request.getOrderAmount().compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                String.format("Minimum order amount of %.2f required", coupon.getMinOrderAmount()));
        }
    }
private void validateUserEligibility(Coupon coupon, Long userId, 
                                     ApplyCouponRequest request, List<String> warnings) {
    
    Long userUsageCount = usageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
    if (userUsageCount >= coupon.getUsagePerUser()) {
        throw new BadRequestException(
            String.format("You have already used this coupon %d times", userUsageCount));
    }

    if (coupon.getIsFirstOrderOnly()) {
        Long userOrderCount = orderRepository.countByUserId(userId);
        if (userOrderCount != null && userOrderCount > 0) {
            throw new BadRequestException("This coupon is valid only for first order");
        }
    }

    if (coupon.getIsNewUserOnly()) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getCreatedAt() != null) {
            Instant userCreatedAt = user.getCreatedAt();
            LocalDateTime userCreatedLocal = LocalDateTime.ofInstant(
                userCreatedAt, ZoneId.systemDefault());
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            if (userCreatedLocal.isBefore(thirtyDaysAgo)) {
                throw new BadRequestException(
                    "This coupon is valid only for new users (joined within 30 days)");
            }
        }
    }
}

    private void validateOrderEligibility(Coupon coupon, ApplyCouponRequest request, List<String> warnings) {
        if (coupon.getApplicablePaymentMethods() != null && 
            !coupon.getApplicablePaymentMethods().isEmpty()) {
            Set<String> allowedMethods = Arrays.stream(coupon.getApplicablePaymentMethods().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
            
            if (!allowedMethods.contains(request.getPaymentMethod())) {
                throw new BadRequestException(
                    "This coupon is valid only for payment methods: " + allowedMethods);
            }
        }
    }

    private void validateProductEligibility(Coupon coupon, ApplyCouponRequest request, List<String> warnings) {
        if (coupon.getExcludedProductIds() != null && !coupon.getExcludedProductIds().isEmpty()) {
            Set<Long> excludedProducts = coupon.getExcludedProductIds();
            if (request.getProductIds() != null) {
                for (Long productId : request.getProductIds()) {
                    if (excludedProducts.contains(productId)) {
                        warnings.add("Some products in your cart are excluded from this coupon");
                    }
                }
            }
        }

        if (coupon.getExcludedCategoryIds() != null && !coupon.getExcludedCategoryIds().isEmpty()) {
            Set<Long> excludedCategories = coupon.getExcludedCategoryIds();
            if (request.getCategoryIds() != null) {
                for (Long categoryId : request.getCategoryIds()) {
                    if (excludedCategories.contains(categoryId)) {
                        warnings.add("Some categories in your cart are excluded from this coupon");
                    }
                }
            }
        }

        if (coupon.getApplicableProductIds() != null && !coupon.getApplicableProductIds().isEmpty()) {
            Set<Long> applicableProducts = coupon.getApplicableProductIds();
            boolean hasApplicableProduct = false;
            
            if (request.getProductIds() != null) {
                for (Long productId : request.getProductIds()) {
                    if (applicableProducts.contains(productId)) {
                        hasApplicableProduct = true;
                        break;
                    }
                }
            }
            
            if (!hasApplicableProduct) {
                throw new BadRequestException("This coupon is not applicable to any product in your cart");
            }
        }

        if (coupon.getApplicableCategoryIds() != null && !coupon.getApplicableCategoryIds().isEmpty()) {
            Set<Long> applicableCategories = coupon.getApplicableCategoryIds();
            boolean hasApplicableCategory = false;
            
            if (request.getCategoryIds() != null) {
                for (Long categoryId : request.getCategoryIds()) {
                    if (applicableCategories.contains(categoryId)) {
                        hasApplicableCategory = true;
                        break;
                    }
                }
            }
            
            if (!hasApplicableCategory) {
                throw new BadRequestException("This coupon is not applicable to any category in your cart");
            }
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        switch (coupon.getType()) {
            case PERCENTAGE:
                return orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            case FIXED:
                return coupon.getDiscountValue().min(orderAmount);  
            
            case FREE_SHIPPING:
                return BigDecimal.ZERO;
            
            default:
                return BigDecimal.ZERO;
        }
    }


    @Transactional(readOnly = true)
    public CouponStatsDto getCouponStats() {
        try {
            CouponStatsDto stats = new CouponStatsDto();

            stats.setTotalCoupons(couponRepository.count());
            
            List<Object[]> statusCounts = couponRepository.countByStatus();
            Map<String, Long> statusMap = new HashMap<>();
            for (Object[] row : statusCounts) {
                CouponStatus status = (CouponStatus) row[0];
                Long count = (Long) row[1];
                statusMap.put(status.name(), count);
                
                switch (status) {
                    case ACTIVE: stats.setActiveCoupons(count); break;
                    case EXPIRED: stats.setExpiredCoupons(count); break;
                    case DISABLED: stats.setDisabledCoupons(count); break;
                }
            }

            List<Object[]> typeCounts = couponRepository.countByType();
            Map<String, Long> typeMap = new HashMap<>();
            for (Object[] row : typeCounts) {
                CouponType type = (CouponType) row[0];
                Long count = (Long) row[1];
                typeMap.put(type.name(), count);
            }
            stats.setUsageByType(typeMap);

            List<CouponUsage> allUsages = usageRepository.findAll();
            stats.setTotalUses((long) allUsages.size());
            stats.setTotalDiscountGiven(allUsages.stream()
                .map(CouponUsage::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

            Pageable pageable = PageRequest.of(0, 10, Sort.by("usedAt").descending());
            Page<CouponUsage> recentPage = usageRepository.findByFilters(null, null, null, null, pageable);
            stats.setRecentUsages(recentPage.getContent().stream()
                .map(this::mapToUsageDto)
                .collect(Collectors.toList()));

            LocalDateTime startOfYear = LocalDateTime.now().withDayOfYear(1);
            List<Object[]> monthlyStats = usageRepository.getDailyUsageStats(
                startOfYear, LocalDateTime.now());
            
            Map<String, BigDecimal> monthlyDiscount = new HashMap<>();
            for (Object[] row : monthlyStats) {
                String date = row[0].toString();
                BigDecimal total = (BigDecimal) row[2];
                monthlyDiscount.put(date, total);
            }
            stats.setDiscountByMonth(monthlyDiscount);

            return stats;

        } catch (Exception e) {
            log.error("Error generating coupon stats", e);
            throw new BusinessException("Failed to generate coupon statistics", "STATS_ERROR");
        }
    }

    @Transactional
    public int expireCoupons() {
        log.info("Running scheduled task to expire coupons");
        int expiredCount = couponRepository.expireCoupons(LocalDateTime.now());
        log.info("Expired {} coupons", expiredCount);
        return expiredCount;
    }

    @Transactional
    public int activateScheduledCoupons() {
        log.info("Running scheduled task to activate scheduled coupons");
        LocalDateTime now = LocalDateTime.now();
       Pageable pageable = PageRequest.of(0, 1000);
    Page<Coupon> scheduledCouponsPage = couponRepository.findByStatus(CouponStatus.SCHEDULED, pageable);
    List<Coupon> scheduledCoupons = scheduledCouponsPage.getContent();
    
        int activatedCount = 0;
        for (Coupon coupon : scheduledCoupons) {
            if (coupon.getValidFrom().isBefore(now) || coupon.getValidFrom().isEqual(now)) {
                coupon.setStatus(CouponStatus.ACTIVE);
                activatedCount++;
            }
        }
        
        if (activatedCount > 0) {
            couponRepository.saveAll(scheduledCoupons);
        }
        
        log.info("Activated {} scheduled coupons", activatedCount);
        return activatedCount;
    }


    @Transactional(readOnly = true)
    public PageResponseDto<CouponUsageDto> getUserCouponHistory(Long userId, Pageable pageable) {
        try {
            Page<CouponUsage> usages = usageRepository.findByUserId(userId, pageable);
            
            return PageResponseDto.<CouponUsageDto>builder()
                .content(usages.getContent().stream()
                    .map(this::mapToUsageDto)
                    .collect(Collectors.toList()))
                .page(usages.getNumber())
                .size(usages.getSize())
                .totalElements(usages.getTotalElements())
                .totalPages(usages.getTotalPages())
                .last(usages.isLast())
                .build();

        } catch (Exception e) {
            log.error("Error fetching user coupon history", e);
            throw new BusinessException("Failed to fetch coupon history", "HISTORY_ERROR");
        }
    }

    @Transactional(readOnly = true)
    public List<CouponDto> getAvailableCouponsForUser(Long userId, BigDecimal orderAmount) {
        try {
            List<Coupon> activeCoupons = couponRepository.findActiveCoupons(LocalDateTime.now());
            
            return activeCoupons.stream()
                .filter(coupon -> {
                    try {
                        ApplyCouponRequest testRequest = ApplyCouponRequest.builder()
                            .userId(userId)
                            .orderAmount(orderAmount)
                            .build();
                        
                        validateCouponBasicEligibility(coupon, testRequest, new ArrayList<>());
                        validateUserEligibility(coupon, userId, testRequest, new ArrayList<>());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching available coupons", e);
            throw new BusinessException("Failed to fetch available coupons", "AVAILABLE_ERROR");
        }
    }


    private void validateDiscountValue(CouponDto dto) {
        if (dto.getType() == CouponType.PERCENTAGE) {
            if (dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0 ||
                dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException("Percentage discount must be between 0 and 100");
            }
        } else if (dto.getType() == CouponType.FIXED) {
            if (dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Fixed discount must be greater than 0");
            }
        }
    }

    private void updateCouponStatus(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        
        if (coupon.getStatus() == CouponStatus.DISABLED) {
            return; 
        }
        
        if (now.isAfter(coupon.getValidTo())) {
            coupon.setStatus(CouponStatus.EXPIRED);
        } else if (now.isBefore(coupon.getValidFrom())) {
            coupon.setStatus(CouponStatus.SCHEDULED);
        } else {
            coupon.setStatus(CouponStatus.ACTIVE);
        }
    }

    private Coupon mapToEntity(CouponDto dto) {
        return Coupon.builder()
            .code(dto.getCode().toUpperCase())
            .description(dto.getDescription())
            .type(dto.getType())
            .discountValue(dto.getDiscountValue())
            .minOrderAmount(dto.getMinOrderAmount())
            .maxDiscountAmount(dto.getMaxDiscountAmount())
            .validFrom(dto.getValidFrom())
            .validTo(dto.getValidTo())
            .usageLimit(dto.getUsageLimit())
            .usagePerUser(dto.getUsagePerUser())
            .status(dto.getStatus() != null ? dto.getStatus() : CouponStatus.SCHEDULED)
            .applicableCategoryIds(dto.getApplicableCategoryIds())
            .applicableProductIds(dto.getApplicableProductIds())
            .excludedCategoryIds(dto.getExcludedCategoryIds())
            .excludedProductIds(dto.getExcludedProductIds())
            .isFirstOrderOnly(dto.getIsFirstOrderOnly())
            .isNewUserOnly(dto.getIsNewUserOnly())
            .applicablePaymentMethods(dto.getApplicablePaymentMethods())
            .applicableUserTiers(dto.getApplicableUserTiers())
            .build();
    }

    private CouponDto mapToDto(Coupon coupon) {
        return CouponDto.builder()
            .id(coupon.getId())
            .code(coupon.getCode())
            .description(coupon.getDescription())
            .type(coupon.getType())
            .discountValue(coupon.getDiscountValue())
            .minOrderAmount(coupon.getMinOrderAmount())
            .maxDiscountAmount(coupon.getMaxDiscountAmount())
            .validFrom(coupon.getValidFrom())
            .validTo(coupon.getValidTo())
            .usageLimit(coupon.getUsageLimit())
            .usagePerUser(coupon.getUsagePerUser())
            .status(coupon.getStatus())
            .totalUsedCount(coupon.getTotalUsedCount())
            .applicableCategoryIds(coupon.getApplicableCategoryIds())
            .applicableProductIds(coupon.getApplicableProductIds())
            .excludedCategoryIds(coupon.getExcludedCategoryIds())
            .excludedProductIds(coupon.getExcludedProductIds())
            .isFirstOrderOnly(coupon.getIsFirstOrderOnly())
            .isNewUserOnly(coupon.getIsNewUserOnly())
            .applicablePaymentMethods(coupon.getApplicablePaymentMethods())
            .applicableUserTiers(coupon.getApplicableUserTiers())
            .createdAt(coupon.getCreatedAt())
            .updatedAt(coupon.getUpdatedAt())
            .build();
    }

    private void updateEntity(Coupon coupon, CouponDto dto) {
        coupon.setDescription(dto.getDescription());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMinOrderAmount(dto.getMinOrderAmount());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidTo(dto.getValidTo());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setUsagePerUser(dto.getUsagePerUser());
        coupon.setApplicableCategoryIds(dto.getApplicableCategoryIds());
        coupon.setApplicableProductIds(dto.getApplicableProductIds());
        coupon.setExcludedCategoryIds(dto.getExcludedCategoryIds());
        coupon.setExcludedProductIds(dto.getExcludedProductIds());
        coupon.setIsFirstOrderOnly(dto.getIsFirstOrderOnly());
        coupon.setIsNewUserOnly(dto.getIsNewUserOnly());
        coupon.setApplicablePaymentMethods(dto.getApplicablePaymentMethods());
        coupon.setApplicableUserTiers(dto.getApplicableUserTiers());
    }

    private CouponUsageDto mapToUsageDto(CouponUsage usage) {
        return CouponUsageDto.builder()
            .id(usage.getId())
            .couponCode(usage.getCoupon().getCode())
            .userEmail(usage.getUser().getEmail())
            .orderNumber(usage.getOrderNumber())
            .discountAmount(usage.getDiscountAmount())
            .usedAt(usage.getUsedAt())
            .wasSuccessful(usage.getWasSuccessful())
            .build();
    }
}