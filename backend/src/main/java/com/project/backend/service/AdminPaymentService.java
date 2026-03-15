package com.project.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.entity.Order;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.PaymentTransaction;
import com.project.backend.entity.Refund;
import com.project.backend.entity.RefundStatus;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.BusinessException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.PaymentTransactionRepository;
import com.project.backend.repository.RefundRepository;
import com.project.backend.repository.UserRepository;
import com.project.backend.requestDto.AdminPaymentStatsDto;
import com.project.backend.requestDto.AdminPaymentTransactionDto;
import com.project.backend.requestDto.AdminRefundDto;
import com.project.backend.requestDto.DailyPaymentSummary;
import com.project.backend.requestDto.HourlyPaymentTrend;
import com.project.backend.requestDto.PageResponseDto;
import com.project.backend.requestDto.PaymentFilterRequest;
import com.project.backend.requestDto.ProcessRefundRequest;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
   // private final NotificationService notificationService;
    private final RazorpayClient razorpayClient;

//  @Transactional(readOnly = true)
//     public PageResponseDto<AdminPaymentTransactionDto> getAllTransactions(
//             PaymentFilterRequest filter, Pageable pageable) {
        
//         log.info("Fetching payments with filters: {}", filter);
        
//         try {
//             Page<PaymentTransaction> transactions = paymentRepository.findByFilters(
//                 filter.getSearch(),
//                 filter.getStatus() != null ? PaymentStatus.valueOf(filter.getStatus()) : null,
//                 filter.getPaymentMethod(),
//                 filter.getUserId(),
//                 filter.getOrderId(),
//                 filter.getFromDate(),
//                 filter.getToDateTime(),
//                 filter.getMinAmount(),
//                 filter.getMaxAmount(),
//                 pageable
//             );

//             return PageResponseDto.<AdminPaymentTransactionDto>builder()
//                     .content(transactions.getContent().stream()
//                             .map(this::convertToDto)
//                             .collect(Collectors.toList()))
//                     .page(transactions.getNumber())
//                     .size(transactions.getSize())
//                     .totalElements(transactions.getTotalElements())
//                     .totalPages(transactions.getTotalPages())
//                     .last(transactions.isLast())
//                     .build();

//         } catch (IllegalArgumentException e) {
//             log.error("Invalid payment status: {}", filter.getStatus());
//             throw new BadRequestException("Invalid payment status: " + filter.getStatus());
//         } catch (Exception e) {
//             log.error("Error fetching payments", e);
//             throw new BusinessException("Failed to fetch payments", "PAYMENT_FETCH_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public AdminPaymentTransactionDto getTransactionById(Long transactionId) {
//         try {
//             PaymentTransaction transaction = paymentRepository.findById(transactionId)
//                     .orElseThrow(() -> new NotFoundException("Payment transaction not found with id: " + transactionId));
            
//             return convertToDto(transaction);
//         } catch (NotFoundException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error fetching transaction: {}", transactionId, e);
//             throw new BusinessException("Failed to fetch transaction", "TRANSACTION_FETCH_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public List<AdminPaymentTransactionDto> getTransactionsByOrder(Long orderId) {
//         try {
//             List<PaymentTransaction> transactions = paymentRepository.findByOrderId(orderId);
//             return transactions.stream()
//                     .map(this::convertToDto)
//                     .collect(Collectors.toList());
//         } catch (Exception e) {
//             log.error("Error fetching transactions for order: {}", orderId, e);
//             throw new BusinessException("Failed to fetch order transactions", "ORDER_TRANSACTIONS_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public PageResponseDto<AdminPaymentTransactionDto> getTransactionsByUser(
//             Long userId, Pageable pageable) {
        
//         try {
//             userRepository.findById(userId)
//                     .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
            
//             Page<PaymentTransaction> transactions = paymentRepository.findByOrder_UserId(userId, pageable);
            
//             return PageResponseDto.<AdminPaymentTransactionDto>builder()
//                     .content(transactions.getContent().stream()
//                             .map(this::convertToDto)
//                             .collect(Collectors.toList()))
//                     .page(transactions.getNumber())
//                     .size(transactions.getSize())
//                     .totalElements(transactions.getTotalElements())
//                     .totalPages(transactions.getTotalPages())
//                     .last(transactions.isLast())
//                     .build();
                    
//         } catch (NotFoundException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error fetching transactions for user: {}", userId, e);
//             throw new BusinessException("Failed to fetch user transactions", "USER_TRANSACTIONS_ERROR");
//         }
//     }

//     // ==================== DASHBOARD STATISTICS ====================

//     @Transactional(readOnly = true)
//     public AdminPaymentStatsDto getPaymentStats() {
//         try {
//             LocalDateTime now = LocalDateTime.now();
//             LocalDateTime startOfDay = now.with(LocalTime.MIN);
//             LocalDateTime startOfWeek = now.minusDays(7).with(LocalTime.MIN);
//             LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);

//             AdminPaymentStatsDto stats = new AdminPaymentStatsDto();
            
//             // Count by status
//             stats.setTotalTransactions(paymentRepository.count());
//             stats.setSuccessfulPayments(paymentRepository.countByStatus(PaymentStatus.SUCCESS));
//             stats.setFailedPayments(paymentRepository.countByStatus(PaymentStatus.FAILED));
//             stats.setPendingPayments(paymentRepository.countByStatus(PaymentStatus.PENDING) 
//                                     + paymentRepository.countByStatus(PaymentStatus.CREATED));
//             stats.setRefundedPayments(paymentRepository.countByStatus(PaymentStatus.REFUNDED));

//             // Revenue
//             stats.setTotalRevenue(paymentRepository.getTotalRevenue());
//             stats.setTodayRevenue(paymentRepository.getRevenueSince(startOfDay));
//             stats.setWeeklyRevenue(paymentRepository.getRevenueSince(startOfWeek));
//             stats.setMonthlyRevenue(paymentRepository.getRevenueSince(startOfMonth));
            
//             // Refunds
//             Double totalRefunded = refundRepository.getTotalRefundedAmount();
//             stats.setTotalRefundedAmount(totalRefunded != null ? 
//                     BigDecimal.valueOf(totalRefunded) : BigDecimal.ZERO);
            
//             // Average transaction value
//             if (stats.getSuccessfulPayments() > 0 && stats.getTotalRevenue() != null) {
//                 stats.setAverageTransactionValue(
//                     stats.getTotalRevenue().divide(
//                         BigDecimal.valueOf(stats.getSuccessfulPayments()), 2, RoundingMode.HALF_UP
//                     ).doubleValue()
//                 );
//             }

//             // Payment method breakdown
//             List<Object[]> methodStats = paymentRepository.getPaymentMethodStats();
//             Map<String, Long> paymentsByMethod = new HashMap<>();
//             Map<String, BigDecimal> revenueByMethod = new HashMap<>();
            
//             for (Object[] row : methodStats) {
//                 String method = (String) row[0];
//                 Long count = (Long) row[1];
//                 BigDecimal total = (BigDecimal) row[2];
//                 paymentsByMethod.put(method != null ? method : "UNKNOWN", count);
//                 revenueByMethod.put(method != null ? method : "UNKNOWN", total);
//             }
//             stats.setPaymentsByMethod(paymentsByMethod);
//             stats.setRevenueByMethod(revenueByMethod);

//             // Status breakdown
//             List<Object[]> statusStats = paymentRepository.getPaymentStatusStats();
//             Map<String, Long> paymentsByStatus = new HashMap<>();
//             for (Object[] row : statusStats) {
//                 PaymentStatus status = (PaymentStatus) row[0];
//                 Long count = (Long) row[1];
//                 paymentsByStatus.put(status.name(), count);
//             }
//             stats.setPaymentsByStatus(paymentsByStatus);

//             // Daily trend for last 30 days
//             List<Object[]> dailyData = paymentRepository.getDailyRevenue(
//                 LocalDateTime.now().minusDays(30), LocalDateTime.now());
            
//             List<DailyPaymentSummary> dailyTrend = dailyData.stream()
//                     .map(row -> DailyPaymentSummary.builder()
//                             .date(row[0].toString())
//                             .count((Long) row[1])
//                             .amount((BigDecimal) row[2])
//                             .build())
//                     .collect(Collectors.toList());
//             stats.setDailyTrend(dailyTrend);

//             return stats;

//         } catch (Exception e) {
//             log.error("Error generating payment stats", e);
//             throw new BusinessException("Failed to generate payment statistics", "STATS_GENERATION_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public AdminPaymentStatsDto getPaymentStatsByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
//         try {
//             // Validate date range
//             if (fromDate.isAfter(toDate)) {
//                 throw new BadRequestException("From date cannot be after to date");
//             }
            
//             // Create a pageable to get all transactions in range
//             Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
//             Page<PaymentTransaction> transactions = paymentRepository.findByFilters(
//                 null, null, null, null, null, fromDate, toDate, null, null, pageable);
            
//             // Calculate stats manually
//             AdminPaymentStatsDto stats = new AdminPaymentStatsDto();
//             stats.setTotalTransactions(transactions.getTotalElements());
            
//             BigDecimal totalRevenue = BigDecimal.ZERO;
//             long successCount = 0;
//             long failedCount = 0;
//             long pendingCount = 0;
            
//             Map<String, Long> methodCount = new HashMap<>();
//             Map<String, BigDecimal> methodRevenue = new HashMap<>();
            
//             for (PaymentTransaction t : transactions) {
//                 if (t.getStatus() == PaymentStatus.SUCCESS) {
//                     successCount++;
//                     totalRevenue = totalRevenue.add(t.getAmount());
                    
//                     String method = t.getPaymentMethod() != null ? t.getPaymentMethod() : "UNKNOWN";
//                     methodCount.put(method, methodCount.getOrDefault(method, 0L) + 1);
//                     methodRevenue.put(method, methodRevenue.getOrDefault(method, BigDecimal.ZERO)
//                             .add(t.getAmount()));
//                 } else if (t.getStatus() == PaymentStatus.FAILED) {
//                     failedCount++;
//                 } else {
//                     pendingCount++;
//                 }
//             }
            
//             stats.setSuccessfulPayments(successCount);
//             stats.setFailedPayments(failedCount);
//             stats.setPendingPayments(pendingCount);
//             stats.setTotalRevenue(totalRevenue);
//             stats.setPaymentsByMethod(methodCount);
//             stats.setRevenueByMethod(methodRevenue);
            
//             if (successCount > 0) {
//                 stats.setAverageTransactionValue(
//                     totalRevenue.divide(BigDecimal.valueOf(successCount), 2, RoundingMode.HALF_UP).doubleValue()
//                 );
//             }
            
//             return stats;
            
//         } catch (BadRequestException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error generating payment stats for date range", e);
//             throw new BusinessException("Failed to generate date range statistics", "DATE_RANGE_STATS_ERROR");
//         }
//     }

//     // ==================== REFUND MANAGEMENT ====================

//     @Transactional
//     public AdminRefundDto processRefund(Long transactionId, ProcessRefundRequest request, User admin) {
//         try {
//             PaymentTransaction transaction = paymentRepository.findById(transactionId)
//                     .orElseThrow(() -> new NotFoundException("Transaction not found"));

//             // Validate refund eligibility
//             if (transaction.getStatus() != PaymentStatus.SUCCESS) {
//                 throw new InvalidStateException("Only successful payments can be refunded");
//             }

//             BigDecimal refundAmount = request.getAmount() != null ? 
//                     request.getAmount() : transaction.getAmount();

//             if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
//                 throw new BadRequestException("Refund amount must be greater than zero");
//             }

//             BigDecimal totalRefunded = transaction.getRefundedAmount() != null ?
//                     transaction.getRefundedAmount() : BigDecimal.ZERO;
            
//             BigDecimal remainingAmount = transaction.getAmount().subtract(totalRefunded);
            
//             if (refundAmount.compareTo(remainingAmount) > 0) {
//                 throw new BadRequestException(String.format(
//                     "Refund amount %.2f exceeds remaining amount %.2f", 
//                     refundAmount, remainingAmount));
//             }

//             // Process with Razorpay
//             com.razorpay.Refund razorpayRefund = null;
//             try {
//                 JSONObject refundRequest = new JSONObject();
//                 refundRequest.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).intValue());
//                 refundRequest.put("speed", "normal");
//                 refundRequest.put("notes", new JSONObject()
//                     .put("reason", request.getReason())
//                     .put("processed_by", admin.getEmail()));
                
//                 razorpayRefund = razorpayClient.payments.refund(
//                     transaction.getRazorpayPaymentId(), refundRequest);
                
//             } catch (RazorpayException e) {
//                 log.error("Razorpay refund failed", e);
//                 throw new PaymentGatewayException("Refund failed at payment gateway: " + e.getMessage());
//             }

//             // Create refund record
//             Refund refund = Refund.builder()
//                     .transaction(transaction)
//                     .razorpayRefundId(razorpayRefund.get("id"))
//                     .amount(refundAmount)
//                     .status(RefundStatus.PROCESSED)
//                     .reason(request.getReason())
//                     .processedAt(LocalDateTime.now())
//                     .processedBy(admin)
//                     .gatewayResponse(razorpayRefund.toString())
//                     .build();
            
//             refundRepository.save(refund);

//             // Update transaction
//             BigDecimal newTotalRefunded = totalRefunded.add(refundAmount);
//             transaction.setRefundedAmount(newTotalRefunded);
            
//             if (newTotalRefunded.compareTo(transaction.getAmount()) >= 0) {
//                 transaction.setStatus(PaymentStatus.REFUNDED);
//             } else {
//                 transaction.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
//             }
            
//             transaction.setRefundedAt(LocalDateTime.now());
//             transaction.setRefundId(razorpayRefund.get("id"));
//             paymentRepository.save(transaction);

//             // Update order status
//             Order order = transaction.getOrder();
//             order.setPaymentStatus(PaymentStatus.REFUNDED);
//             orderRepository.save(order);

//             // Send notification
//             if (Boolean.TRUE.equals(request.getNotifyCustomer())) {
//                 notificationService.sendRefundNotification(
//                     order.getUser(), order, refundAmount, request.getReason());
//             }

//             // Audit log
//             auditService.log(admin, "PROCESSED_REFUND", 
//                 String.format("Refund of ₹%s for transaction %d", refundAmount, transactionId));

//             log.info("Refund processed successfully: transaction={}, amount={}, admin={}", 
//                     transactionId, refundAmount, admin.getEmail());

//             return convertToRefundDto(refund);

//         } catch (NotFoundException | InvalidStateException | BadRequestException | PaymentGatewayException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error processing refund for transaction: {}", transactionId, e);
//             throw new BusinessException("Failed to process refund", "REFUND_PROCESSING_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public PageResponseDto<AdminRefundDto> getAllRefunds(
//             String status, Long transactionId, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        
//         try {
//             RefundStatus refundStatus = status != null ? RefundStatus.valueOf(status) : null;
            
//             Page<Refund> refunds = refundRepository.findByFilters(
//                 refundStatus, transactionId, fromDate, toDate, pageable);
            
//             return PageResponseDto.<AdminRefundDto>builder()
//                     .content(refunds.getContent().stream()
//                             .map(this::convertToRefundDto)
//                             .collect(Collectors.toList()))
//                     .page(refunds.getNumber())
//                     .size(refunds.getSize())
//                     .totalElements(refunds.getTotalElements())
//                     .totalPages(refunds.getTotalPages())
//                     .last(refunds.isLast())
//                     .build();
                    
//         } catch (IllegalArgumentException e) {
//             throw new BadRequestException("Invalid refund status: " + status);
//         } catch (Exception e) {
//             log.error("Error fetching refunds", e);
//             throw new BusinessException("Failed to fetch refunds", "REFUND_FETCH_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public AdminRefundDto getRefundById(Long refundId) {
//         try {
//             Refund refund = refundRepository.findById(refundId)
//                     .orElseThrow(() -> new NotFoundException("Refund not found with id: " + refundId));
            
//             return convertToRefundDto(refund);
//         } catch (NotFoundException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error fetching refund: {}", refundId, e);
//             throw new BusinessException("Failed to fetch refund", "REFUND_FETCH_ERROR");
//         }
//     }

//     // ==================== PAYMENT OPERATIONS ====================

//     @Transactional
//     public void retryPayment(Long transactionId, User admin) {
//         try {
//             PaymentTransaction transaction = paymentRepository.findById(transactionId)
//                     .orElseThrow(() -> new NotFoundException("Transaction not found"));

//             if (transaction.getStatus() != PaymentStatus.FAILED) {
//                 throw new InvalidStateException("Only failed payments can be retried");
//             }

//             // Check retry limit
//             if (transaction.getRetryCount() >= 3) {
//                 throw new BusinessException("Maximum retry limit reached (3 attempts)", "RETRY_LIMIT_EXCEEDED");
//             }

//             // Update transaction
//             transaction.setStatus(PaymentStatus.CREATED);
//             transaction.setRetryCount(transaction.getRetryCount() + 1);
//             transaction.setLastRetryAt(LocalDateTime.now());
//             transaction.setFailureReason(null);
//             paymentRepository.save(transaction);

//             // Notify user
//             notificationService.sendPaymentRetryNotification(
//                 transaction.getOrder().getUser(), transaction.getOrder());

//             log.info("Payment retry initiated: transaction={}, retryCount={}, admin={}",
//                     transactionId, transaction.getRetryCount(), admin.getEmail());

//         } catch (NotFoundException | InvalidStateException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error retrying payment: {}", transactionId, e);
//             throw new BusinessException("Failed to retry payment", "RETRY_PAYMENT_ERROR");
//         }
//     }

//     @Transactional
//     public AdminPaymentTransactionDto markPaymentAsFailed(Long transactionId, String reason, User admin) {
//         try {
//             PaymentTransaction transaction = paymentRepository.findById(transactionId)
//                     .orElseThrow(() -> new NotFoundException("Transaction not found"));

//             if (transaction.getStatus() == PaymentStatus.SUCCESS) {
//                 throw new InvalidStateException("Cannot mark successful payment as failed");
//             }

//             transaction.setStatus(PaymentStatus.FAILED);
//             transaction.setFailureReason(reason);
//             transaction.setGatewayResponse("Manually marked as failed by admin: " + admin.getEmail());
//             paymentRepository.save(transaction);

//             // Update order
//             Order order = transaction.getOrder();
//             order.setPaymentStatus(PaymentStatus.FAILED);
//             orderRepository.save(order);

//             // Audit log
//             auditService.log(admin, "MARKED_PAYMENT_FAILED", 
//                 String.format("Transaction %d marked as failed. Reason: %s", transactionId, reason));

//             log.info("Payment marked as failed: transaction={}, admin={}, reason={}",
//                     transactionId, admin.getEmail(), reason);

//             return convertToDto(transaction);

//         } catch (NotFoundException | InvalidStateException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error marking payment as failed: {}", transactionId, e);
//             throw new BusinessException("Failed to mark payment as failed", "MARK_FAILED_ERROR");
//         }
//     }

//     @Transactional
//     public AdminPaymentTransactionDto verifyPaymentManually(Long transactionId, User admin) {
//         try {
//             PaymentTransaction transaction = paymentRepository.findById(transactionId)
//                     .orElseThrow(() -> new NotFoundException("Transaction not found"));

//             if (transaction.getRazorpayPaymentId() == null) {
//                 throw new InvalidStateException("No payment ID associated with this transaction");
//             }

//             // Fetch payment details from Razorpay
//             try {
//                 Payment payment = razorpayClient.payments.fetch(transaction.getRazorpayPaymentId());
                
//                 if ("captured".equals(payment.get("status"))) {
//                     transaction.setStatus(PaymentStatus.SUCCESS);
//                     transaction.setCompletedAt(LocalDateTime.now());
                    
//                     // Update order
//                     Order order = transaction.getOrder();
//                     order.setPaymentStatus(PaymentStatus.SUCCESS);
//                     orderRepository.save(order);
                    
//                     log.info("Payment manually verified as successful: transaction={}, admin={}",
//                             transactionId, admin.getEmail());
//                 } else {
//                     throw new BusinessException("Payment is not in captured state", "PAYMENT_NOT_CAPTURED");
//                 }
                
//             } catch (RazorpayException e) {
//                 log.error("Failed to fetch payment from Razorpay", e);
//                 throw new PaymentGatewayException("Failed to verify payment with gateway");
//             }

//             paymentRepository.save(transaction);
            
//             // Audit log
//             auditService.log(admin, "MANUALLY_VERIFIED_PAYMENT", 
//                 "Transaction " + transactionId + " manually verified");

//             return convertToDto(transaction);

//         } catch (NotFoundException | InvalidStateException | PaymentGatewayException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error verifying payment manually: {}", transactionId, e);
//             throw new BusinessException("Failed to verify payment", "VERIFICATION_ERROR");
//         }
//     }

//     // ==================== ANALYTICS ====================

//     @Transactional(readOnly = true)
//     public List<PaymentMethodAnalytics> getPaymentMethodAnalytics(LocalDateTime fromDate, LocalDateTime toDate) {
//         try {
//             // Create pageable to get all data
//             Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
//             Page<PaymentTransaction> transactions = paymentRepository.findByFilters(
//                 null, null, null, null, null, fromDate, toDate, null, null, pageable);
            
//             Map<String, PaymentMethodAnalytics> analyticsMap = new HashMap<>();
            
//             for (PaymentTransaction t : transactions) {
//                 if (t.getStatus() == PaymentStatus.SUCCESS) {
//                     String method = t.getPaymentMethod() != null ? t.getPaymentMethod() : "UNKNOWN";
                    
//                     PaymentMethodAnalytics analytics = analyticsMap.computeIfAbsent(method,
//                         k -> PaymentMethodAnalytics.builder().method(k).build());
                    
//                     analytics.setTransactionCount(analytics.getTransactionCount() + 1);
//                     analytics.setTotalAmount(analytics.getTotalAmount() != null ?
//                             analytics.getTotalAmount().add(t.getAmount()) : t.getAmount());
//                 }
//             }
            
//             // Calculate success rates and averages
//             for (PaymentMethodAnalytics analytics : analyticsMap.values()) {
//                 long totalForMethod = transactions.getContent().stream()
//                         .filter(t -> methodEquals(t.getPaymentMethod(), analytics.getMethod()))
//                         .count();
                
//                 if (totalForMethod > 0) {
//                     analytics.setSuccessRate(
//                         (double) analytics.getTransactionCount() / totalForMethod * 100);
//                 }
                
//                 if (analytics.getTransactionCount() > 0 && analytics.getTotalAmount() != null) {
//                     analytics.setAverageAmount(
//                         analytics.getTotalAmount().divide(
//                             BigDecimal.valueOf(analytics.getTransactionCount()),
//                             2, RoundingMode.HALF_UP).doubleValue());
//                 }
//             }
            
//             return new ArrayList<>(analyticsMap.values());
            
//         } catch (Exception e) {
//             log.error("Error generating payment method analytics", e);
//             throw new BusinessException("Failed to generate payment method analytics", "ANALYTICS_ERROR");
//         }
//     }

//     @Transactional(readOnly = true)
//     public List<HourlyPaymentTrend> getHourlyPaymentTrend(LocalDateTime date) {
//         try {
//             List<Object[]> hourlyData = paymentRepository.getHourlyRevenue(date);
            
//             return hourlyData.stream()
//                     .map(row -> HourlyPaymentTrend.builder()
//                             .hour((Integer) row[0])
//                             .count((Long) row[1])
//                             .amount((BigDecimal) row[2])
//                             .build())
//                     .collect(Collectors.toList());
                    
//         } catch (Exception e) {
//             log.error("Error generating hourly payment trend for date: {}", date, e);
//             throw new BusinessException("Failed to generate hourly trend", "HOURLY_TREND_ERROR");
//         }
//     }

//     // ==================== REPORT EXPORT ====================

//     public byte[] exportPaymentReport(String format, LocalDateTime fromDate, LocalDateTime toDate) {
//         try {
//             Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
//             Page<PaymentTransaction> transactions = paymentRepository.findByFilters(
//                 null, null, null, null, null, fromDate, toDate, null, null, pageable);
            
//             switch (format.toUpperCase()) {
//                 case "CSV":
//                     return generateCsvReport(transactions.getContent());
//                 case "EXCEL":
//                     return generateExcelReport(transactions.getContent());
//                 case "PDF":
//                     return generatePdfReport(transactions.getContent());
//                 default:
//                     throw new BadRequestException("Unsupported format: " + format);
//             }
            
//         } catch (BadRequestException e) {
//             throw e;
//         } catch (Exception e) {
//             log.error("Error generating payment report", e);
//             throw new BusinessException("Failed to generate report", "REPORT_GENERATION_ERROR");
//         }
//     }

//     private byte[] generateCsvReport(List<PaymentTransaction> transactions) {
//         StringBuilder csv = new StringBuilder();
//         csv.append("Transaction ID,Order ID,Razorpay Order ID,Razorpay Payment ID,Amount,Currency,Status,Payment Method,Created At,Completed At,Refunded Amount\n");
        
//         for (PaymentTransaction t : transactions) {
//             csv.append(String.format("%d,%d,%s,%s,%.2f,%s,%s,%s,%s,%s,%.2f\n",
//                 t.getId(),
//                 t.getOrder().getId(),
//                 t.getRazorpayOrderId(),
//                 t.getRazorpayPaymentId() != null ? t.getRazorpayPaymentId() : "",
//                 t.getAmount(),
//                 t.getCurrency() != null ? t.getCurrency() : "INR",
//                 t.getStatus(),
//                 t.getPaymentMethod() != null ? t.getPaymentMethod() : "",
//                 t.getCreatedAt() != null ? t.getCreatedAt().toString() : "",
//                 t.getCompletedAt() != null ? t.getCompletedAt().toString() : "",
//                 t.getRefundedAmount() != null ? t.getRefundedAmount() : BigDecimal.ZERO
//             ));
//         }
        
//         return csv.toString().getBytes();
//     }

//     private byte[] generateExcelReport(List<PaymentTransaction> transactions) {
//         // Implementation using Apache POI or similar
//         throw new UnsupportedOperationException("Excel export not implemented");
//     }

//     private byte[] generatePdfReport(List<PaymentTransaction> transactions) {
//         // Implementation using iText or similar
//         throw new UnsupportedOperationException("PDF export not implemented");
//     }

//     // ==================== CONVERSION METHODS ====================

//     private AdminPaymentTransactionDto convertToDto(PaymentTransaction transaction) {
//         if (transaction == null) return null;
        
//         Order order = transaction.getOrder();
//         User user = order != null ? order.getUser() : null;
        
//         return AdminPaymentTransactionDto.builder()
//                 .id(transaction.getId())
//                 .orderId(order != null ? order.getId() : null)
//                 .orderNumber(order != null ? order.getOrderNumber() : null)
//                 .userId(user != null ? user.getId() : null)
//                 .userEmail(user != null ? user.getEmail() : null)
//                 .userName(user != null ? user.getName() : null)
//                 .razorpayOrderId(transaction.getRazorpayOrderId())
//                 .razorpayPaymentId(transaction.getRazorpayPaymentId())
//                 .razorpaySignature(transaction.getRazorpaySignature())
//                 .amount(transaction.getAmount())
//                 .currency(transaction.getCurrency())
//                 .paymentMethod(transaction.getPaymentMethod())
//                 .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
//                 .createdAt(transaction.getCreatedAt())
//                 .completedAt(transaction.getCompletedAt())
//                 .updatedAt(transaction.getUpdatedAt())
//                 .failureReason(transaction.getFailureReason())
//                 .gatewayResponse(transaction.getGatewayResponse())
//                 .refundedAmount(transaction.getRefundedAmount())
//                 .refundedAt(transaction.getRefundedAt())
//                 .refundId(transaction.getRefundId())
//                 .build();
//     }

//     private AdminRefundDto convertToRefundDto(Refund refund) {
//         if (refund == null) return null;
        
//         PaymentTransaction transaction = refund.getTransaction();
//         User processedBy = refund.getProcessedBy();
        
//         return AdminRefundDto.builder()
//                 .id(refund.getId())
//                 .transactionId(transaction != null ? transaction.getId() : null)
//                 .razorpayRefundId(refund.getRazorpayRefundId())
//                 .amount(refund.getAmount())
//                 .status(refund.getStatus() != null ? refund.getStatus().name() : null)
//                 .reason(refund.getReason())
//                 .requestedAt(refund.getRequestedAt())
//                 .processedAt(refund.getProcessedAt())
//                 .processedBy(processedBy != null ? processedBy.getId() : null)
//                 .processedByName(processedBy != null ? processedBy.getName() : null)
//                 .failureReason(refund.getFailureReason())
//                 .build();
//     }

//     private boolean methodEquals(String method1, String method2) {
//         if (method1 == null && method2 == null) return true;
//         if (method1 == null || method2 == null) return false;
//         return method1.equals(method2);
//     }
}
