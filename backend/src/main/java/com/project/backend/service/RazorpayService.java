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
import com.project.backend.repository.OrderRepository;
import com.project.backend.repository.PaymentTransactionRepository;
import com.project.backend.requestDto.CreateOrderRequest;
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

    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * Create Razorpay order for PREPAID orders
     */
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, User user) {
        try {
            // Fetch order
            com.project.backend.entity.Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Verify order belongs to user
            if (!order.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to order");
            }
            
            // Check if order is already paid
            if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
                throw new RuntimeException("Order already paid");
            }
            
            // Ensure it's a PREPAID order
            if (order.getPaymentMethod() != PaymentMethod.PREPAID) {
                throw new RuntimeException("Order is not prepaid");
            }
            
            // Check if transaction already exists
            paymentTransactionRepository.findByOrderId(order.getId())
                .ifPresent(transaction -> {
                    if (!"FAILED".equals(transaction.getStatus())) {
                        throw new RuntimeException("Payment already initiated for this order");
                    }
                });
            
            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(new BigDecimal(100)).intValue()); // Convert to paise
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "receipt_" + order.getId());
            
            JSONObject notes = new JSONObject();
            notes.put("order_id", order.getId());
            notes.put("user_id", user.getId());
            notes.put("user_email", user.getEmail());
            orderRequest.put("notes", notes);
            
            orderRequest.put("partial_payment", false);
            
            log.info("Creating Razorpay order for order ID: {}", order.getId());
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            
            // Save payment transaction
            PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("CREATED")
                .gatewayResponse(razorpayOrder.toString())
                .build();
            
            paymentTransactionRepository.save(transaction);
            
            // Update order payment status to PENDING
            order.setPaymentStatus(PaymentStatus.PENDING);
            orderRepository.save(order);
            
            return CreateOrderResponse.builder()
                .razorpayOrderId(razorpayOrder.get("id"))
                .razorpayKeyId(razorpayKeyId)
                .orderId(order.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .customerPhone(request.getCustomerPhone())
                .build();
            
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed", e);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * Verify payment signature
     */
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, User user) {
        try {
            // Fetch transaction
            PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            com.project.backend.entity.Order order = transaction.getOrder();
            
            // Verify order belongs to user
            if (!order.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access");
            }
            
            // Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());
            
            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            
            if (isValid) {
                // Update transaction
                transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
                transaction.setRazorpaySignature(request.getRazorpaySignature());
                transaction.setStatus("PAID");
                transaction.setCompletedAt(Instant.now());
                
                // Fetch payment details from Razorpay
                com.razorpay.Payment payment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
                JSONObject paymentJson = payment.toJson();
                
                if (paymentJson.has("method")) {
                    transaction.setRazorpayPaymentMethod(paymentJson.getString("method"));
                }
                
                paymentTransactionRepository.save(transaction);
                
                // Update order - using your existing enums
                order.setPaymentStatus(PaymentStatus.SUCCESS); // Using SUCCESS from your enum
                order.setStatus(OrderStatus.PLACED); // Move to PLACED after payment
                orderRepository.save(order);
                
                log.info("Payment verified successfully for order: {}", order.getId());
                
                return PaymentResponse.builder()
                    .success(true)
                    .message("Payment successful")
                    .orderId(order.getId())
                    .paymentId(request.getRazorpayPaymentId())
                    .orderStatus(order.getStatus().name())
                    .paymentStatus(order.getPaymentStatus().name())
                    .build();
            } else {
                // Failed verification
                transaction.setStatus("FAILED");
                transaction.setFailureReason("Signature verification failed");
                paymentTransactionRepository.save(transaction);
                
                // Update order status using your enum
                order.setPaymentStatus(PaymentStatus.FAILED); // Using FAILED from your enum
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

    /**
     * Handle webhook from Razorpay
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        try {
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            
            log.info("Received Razorpay webhook: {}", event);
            
            JSONObject paymentEntity = webhookData.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
            
            String razorpayOrderId = paymentEntity.getString("order_id");
            String razorpayPaymentId = paymentEntity.getString("id");
            
            PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            com.project.backend.entity.Order order = transaction.getOrder();
            
            switch (event) {
                case "payment.captured":
                case "payment.authorized":
                    transaction.setRazorpayPaymentId(razorpayPaymentId);
                    transaction.setStatus("PAID");
                    transaction.setCompletedAt(Instant.now());
                    if (paymentEntity.has("method")) {
                        transaction.setRazorpayPaymentMethod(paymentEntity.getString("method"));
                    }
                    
                    // Update order with your enums
                    order.setPaymentStatus(PaymentStatus.SUCCESS);
                    order.setStatus(OrderStatus.PLACED);
                    break;
                    
                case "payment.failed":
                    String errorDescription = paymentEntity.optString("error_description");
                    transaction.setStatus("FAILED");
                    transaction.setFailureReason(errorDescription);
                    
                    // Update order with your enums
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    break;
            }
            
            paymentTransactionRepository.save(transaction);
            orderRepository.save(order);
            
        } catch (Exception e) {
            log.error("Webhook processing error", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
}