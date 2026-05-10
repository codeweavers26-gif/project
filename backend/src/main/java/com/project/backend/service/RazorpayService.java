package com.project.backend.service;
import java.math.BigDecimal;
import java.time.Instant;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CreateOrderResponse;
import com.project.backend.ResponseDto.PaymentResponse;
import com.project.backend.entity.OrderStatus;
import com.project.backend.entity.PaymentMethod;
import com.project.backend.entity.PaymentStatus;
import com.project.backend.entity.PaymentTransaction;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.exception.UnauthorizedException;
import com.project.backend.entity.OrderStatusHistory;
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.OrderStatusHistoryRepository;
import com.project.backend.repository.PaymentTransactionRepository;
import com.project.backend.requestDto.PaymentRequest;
import com.project.backend.requestDto.VerifyPaymentRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final @org.springframework.lang.Nullable RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final EmailService emailService;
    
    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

 
    @Transactional
    public CreateOrderResponse createOrder(PaymentRequest request, User user) {
        if (razorpayClient == null) {
            throw new BadRequestException("Payment gateway is not configured. Please contact support.");
        }

        com.project.backend.entity.Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized access to order");
        }

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Order is already paid");
        }

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new BadRequestException("COD orders cannot be paid online");
        }

        if (order.getPaymentMethod() != PaymentMethod.PREPAID) {
            throw new BadRequestException("Order is not a prepaid order");
        }

        java.util.Optional<PaymentTransaction> existingTransactionOpt =
            paymentTransactionRepository.findByOrderId(order.getId());

        existingTransactionOpt.ifPresent(existing -> {
            if (!"FAILED".equals(existing.getStatus()) && !"CREATED".equals(existing.getStatus())) {
                throw new BadRequestException("Payment already initiated for this order");
            }
        });

        try {
            int amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");
            orderRequest.put("receipt", request.getReceipt() != null ? request.getReceipt() : "receipt_" + order.getId());

            JSONObject notes = new JSONObject();
            notes.put("order_id", order.getId());
            notes.put("user_id", user.getId());
            notes.put("user_email", user.getEmail());
            orderRequest.put("notes", notes);
            orderRequest.put("partial_payment", false);

            log.info("Creating Razorpay order for order ID: {}", order.getId());
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // Reuse existing transaction (CREATED/FAILED) to avoid unique constraint violation on order_id
            PaymentTransaction transaction = existingTransactionOpt.orElseGet(() ->
                PaymentTransaction.builder().order(order).build()
            );
            transaction.setRazorpayOrderId(razorpayOrder.get("id"));
            transaction.setAmount(request.getAmount());
            transaction.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
            transaction.setStatus("CREATED");
            transaction.setGatewayResponse(razorpayOrder.toString());
            transaction.setFailureReason(null);

            paymentTransactionRepository.save(transaction);

            order.setPaymentStatus(PaymentStatus.PENDING);
            orderRepository.save(order);

            return CreateOrderResponse.builder()
                .razorpayOrderId(razorpayOrder.get("id"))
                .razorpayKeyId(razorpayKeyId)
                .orderId(order.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .customerPhone(request.getCustomerPhone())
                .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed", e);
            throw new BadRequestException("Payment gateway error: " + e.getMessage());
        } catch (BadRequestException | NotFoundException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating payment order", e);
            throw new BadRequestException("Failed to initiate payment. Please try again.");
        }
    }

  
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, User user) {
        if (razorpayClient == null) {
            return PaymentResponse.builder()
                .success(false)
                .message("Payment gateway is not configured")
                .orderId(request.getOrderId())
                .build();
        }
        try {
            PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            com.project.backend.entity.Order order = transaction.getOrder();
            
            if (!order.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access");
            }
            System.err.println(request.getRazorpaySignature());
            System.err.println(request.getRazorpayPaymentId());
            System.err.println(request.getRazorpayOrderId());

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());
            
            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            
            if (isValid) {
                transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
                transaction.setRazorpaySignature(request.getRazorpaySignature());
                transaction.setStatus("PAID");
                transaction.setCompletedAt(Instant.now());
                
                com.razorpay.Payment payment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
                JSONObject paymentJson = payment.toJson();
                
                if (paymentJson.has("method")) {
                    transaction.setRazorpayPaymentMethod(paymentJson.getString("method"));
                }
                
                paymentTransactionRepository.save(transaction);
                
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);

                // Auto-record PLACED + PROCESSING history for prepaid order
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.util.List<OrderStatusHistory> historyEntries = new java.util.ArrayList<>();
                OrderStatusHistory placed = new OrderStatusHistory();
                placed.setOrder(order);
                placed.setStatus(OrderStatus.PLACED.name());
                placed.setChangedAt(now.minusMinutes(1));
                historyEntries.add(placed);
                OrderStatusHistory processing = new OrderStatusHistory();
                processing.setOrder(order);
                processing.setStatus(OrderStatus.PROCESSING.name());
                processing.setChangedAt(now);
                historyEntries.add(processing);
                orderStatusHistoryRepository.saveAll(historyEntries);

                log.info("Payment verified successfully for order: {}", order.getId());

                // Notify customer that payment is confirmed and order is placed
                try {
                    emailService.sendOrderStatusUpdate(order.getUser(), order);
                } catch (Exception ex) {
                    log.warn("Failed to send payment confirmation email for orderId={}: {}", order.getId(), ex.getMessage());
                }

                return PaymentResponse.builder()
                    .success(true)
                    .message("Payment successful")
                    .orderId(order.getId())
                    .paymentId(request.getRazorpayPaymentId())
                    .orderStatus(order.getStatus().name())
                    .paymentStatus(order.getPaymentStatus().name())
                    .build();
            } else {
                transaction.setStatus("FAILED");
                transaction.setFailureReason("Signature verification failed");
                paymentTransactionRepository.save(transaction);
                
                order.setPaymentStatus(PaymentStatus.FAILED); 
                orderRepository.save(order);
                
                log.warn("Payment signature verification failed for order: {}", request.getOrderId());
                
                return PaymentResponse.builder()
                    .success(false)
                    .message("Payment verification failed")
                    .orderId(request.getOrderId())
                    .paymentStatus(PaymentStatus.FAILED.name())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Payment verification error", e);
            return PaymentResponse.builder()
                .success(false)
                .message("Payment verification error: " + e.getMessage())
                .orderId(request.getOrderId())
                .build();
        }
    }

@Transactional
public String refundPayment(String razorpayPaymentId, BigDecimal amount) {

    try {
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", amountInPaise);

        com.razorpay.Refund refund =
                razorpayClient.payments.refund(razorpayPaymentId, refundRequest);

        String refundId = refund.get("id");

        log.info("Refund success: paymentId={}, refundId={}",
                razorpayPaymentId, refundId);

        return refundId;

    } catch (RazorpayException e) {
        log.error("Refund failed for paymentId={}", razorpayPaymentId, e);
        throw new RuntimeException("Refund failed: " + e.getMessage());
    }
}
    
    @Transactional
    public void handleWebhook(String payload, String signature) {
        try {
            
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            JSONObject paymentEntity = webhookData.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            
            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");
            String status = paymentEntity.getString("status");
            
            PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
            
            switch (event) {
                case "payment.captured":
                    transaction.setRazorpayPaymentId(razorpayPaymentId);
                    transaction.setStatus("COMPLETED");
                    transaction.setCompletedAt(Instant.now());
                    
                    com.project.backend.entity.Order order = transaction.getOrder();
                    order.setPaymentStatus(com.project.backend.entity.PaymentStatus.COMPLETED);
                    orderRepository.save(order);
                    break;
                    
                case "payment.failed":
                    transaction.setStatus("FAILED");
                    transaction.setFailureReason(paymentEntity.optString("error_description"));
                    break;
            }
            
            transaction.setGatewayResponse(payload);
            paymentTransactionRepository.save(transaction);
            
            log.info("Webhook processed: {} for order {}", event, razorpayOrderId);

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            throw new BadRequestException("Webhook processing failed");
        }
    }
    
    
}