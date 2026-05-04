package com.project.backend.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.Return;
import com.project.backend.entity.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${sendgrid.from-email:}")
    private String fromEmail;

    // ─────────────────────────────────────────────
    //  OTP
    // ─────────────────────────────────────────────

    @Async
    public void sendOtp(String to, String otp) {

        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping email to {}", to);
            return;
        }

        try {
            Email from = new Email(fromEmail);
            Email recipient = new Email(to);

            String subject = "Your OTP - Rich and Retired";
            String body = buildOtpHtml(otp);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("OTP email sent to {} via SendGrid", to);
            } else {
                log.error("SendGrid failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("SendGrid exception for {}: {}", to, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  ORDER CONFIRMATION (with Invoice)
    // ─────────────────────────────────────────────

    @Async
    public void sendOrderConfirmation(User user, Order order, List<OrderItem> items) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            log.info("Skipping order confirmation email — user {} has no email (phone-only)", user.getId());
            return;
        }

        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping order confirmation to {}", to);
            return;
        }

        try {
            Email from    = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);

            String subject = "Order Confirmed #" + order.getId() + " — Rich & Retired";
            String body    = buildOrderConfirmationHtml(user, order, items);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Order confirmation email sent to {} for orderId={}", to, order.getId());
            } else {
                log.error("SendGrid order confirmation failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("SendGrid order confirmation exception for orderId={}: {}", order.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  ORDER CANCELLATION
    // ─────────────────────────────────────────────

    @Async
    public void sendOrderCancellationEmail(User user, Order order) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            log.info("Skipping cancellation email — user {} has no email", user.getId());
            return;
        }

        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping cancellation email to {}", to);
            return;
        }

        try {
            Email from      = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);

            String subject = "Order #" + order.getId() + " Cancelled — Rich & Retired";
            String body    = buildCancellationHtml(user, order);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Cancellation email sent to {} for orderId={}", to, order.getId());
            } else {
                log.error("SendGrid cancellation failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("SendGrid cancellation exception for orderId={}: {}", order.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  ORDER STATUS UPDATE
    // ─────────────────────────────────────────────

    @Async
    public void sendOrderStatusUpdate(User user, Order order) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            log.info("Skipping status update email — user {} has no email", user.getId());
            return;
        }

        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping status update to {}", to);
            return;
        }

        try {
            Email from    = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);

            String statusLabel = friendlyStatus(order.getStatus().name());
            String subject = "Order #" + order.getId() + " Update: " + statusLabel + " — Rich & Retired";
            String body    = buildStatusUpdateHtml(user, order, statusLabel);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Status update email sent to {} for orderId={} status={}", to, order.getId(), order.getStatus());
            } else {
                log.error("SendGrid status update failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("SendGrid status update exception for orderId={}: {}", order.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  RETURN REQUEST RECEIVED
    // ─────────────────────────────────────────────

    @Async
    public void sendReturnRequestEmail(User user, Return returnRecord) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) return;
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping return request email to {}", to);
            return;
        }

        try {
            Email from      = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);
            String subject  = "Return Request Received #" + returnRecord.getId() + " — Rich & Retired";
            String body     = buildReturnRequestHtml(user, returnRecord);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));
            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Return request email sent to {} for returnId={}", to, returnRecord.getId());
            } else {
                log.error("SendGrid return request failed: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Return request email exception for returnId={}: {}", returnRecord.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  RETURN APPROVED
    // ─────────────────────────────────────────────

    @Async
    public void sendReturnApprovedEmail(User user, Return returnRecord) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) return;
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping return approved email to {}", to);
            return;
        }

        try {
            Email from      = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);
            String subject  = "Return Approved — Pickup Scheduled #" + returnRecord.getId() + " — Rich & Retired";
            String body     = buildReturnApprovedHtml(user, returnRecord);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));
            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Return approved email sent to {} for returnId={}", to, returnRecord.getId());
            } else {
                log.error("SendGrid return approved failed: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Return approved email exception for returnId={}: {}", returnRecord.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  RETURN REJECTED
    // ─────────────────────────────────────────────

    @Async
    public void sendReturnRejectedEmail(User user, Return returnRecord, String rejectionReason) {

        String to = user.getEmail();
        if (to == null || to.isBlank()) return;
        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping return rejected email to {}", to);
            return;
        }

        try {
            Email from      = new Email(fromEmail, "Rich & Retired");
            Email recipient = new Email(to);
            String subject  = "Return Request Update #" + returnRecord.getId() + " — Rich & Retired";
            String body     = buildReturnRejectedHtml(user, returnRecord, rejectionReason);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));
            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("Return rejected email sent to {} for returnId={}", to, returnRecord.getId());
            } else {
                log.error("SendGrid return rejected failed: status={}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Return rejected email exception for returnId={}: {}", returnRecord.getId(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  HTML BUILDERS
    // ─────────────────────────────────────────────

    private String buildOtpHtml(String otp) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #eee;border-radius:8px;">
                  <h2 style="color:#0a0a0a;letter-spacing:2px;text-transform:uppercase;font-weight:300;">Rich &amp; Retired</h2>
                  <p style="color:#555;font-size:15px;">Your one-time password (OTP) is:</p>
                  <div style="font-size:36px;font-weight:bold;letter-spacing:8px;text-align:center;padding:20px 0;color:#0a0a0a;">
                    %s
                  </div>
                  <p style="color:#888;font-size:13px;">This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                  <p style="color:#bbb;font-size:11px;text-align:center;">Rich and Retired &nbsp;|&nbsp; richnretired.com</p>
                </div>
                """.formatted(otp);
    }

    private String buildOrderConfirmationHtml(User user, Order order, List<OrderItem> items) {

        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        String orderId  = "#" + order.getId();
        String orderDate = order.getCreatedAt() != null
                ? DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                        .withZone(ZoneId.of("Asia/Kolkata"))
                        .format(order.getCreatedAt())
                : "—";

        // Items rows
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : items) {
            double lineTotal = item.getPrice() * item.getQuantity();
            String variant = "";
            if (item.getSize() != null)  variant += "Size: " + item.getSize();
            if (item.getColor() != null) variant += (variant.isEmpty() ? "" : " | ") + "Color: " + item.getColor();
            rows.append("""
                    <tr>
                      <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0;">
                        <div style="font-weight:600;color:#1a1a1a;">%s</div>
                        <div style="font-size:12px;color:#888;margin-top:2px;">%s</div>
                      </td>
                      <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0;text-align:center;color:#555;">%d</td>
                      <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0;text-align:right;color:#1a1a1a;">₹%.2f</td>
                      <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0;text-align:right;font-weight:600;color:#1a1a1a;">₹%.2f</td>
                    </tr>
                    """.formatted(item.getProductName(), variant, item.getQuantity(), item.getPrice(), lineTotal));
        }

        // Address
        String address = order.getDeliveryAddressLine1()
                + (order.getDeliveryAddressLine2() != null && !order.getDeliveryAddressLine2().isBlank()
                        ? ", " + order.getDeliveryAddressLine2() : "")
                + ", " + order.getDeliveryCity()
                + ", " + order.getDeliveryState()
                + " - " + order.getDeliveryPostalCode();

        // Tax is included in price; subtotal = total - shipping + discount
        double subtotal = order.getTotalAmount()
                - (order.getShippingCharges() != null ? order.getShippingCharges() : 0)
                + (order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);

        String paymentLabel = order.getPaymentMethod() != null ? order.getPaymentMethod().name().replace("_", " ") : "—";

        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:600px;margin:auto;background:#fff;">

                  <!-- Header -->
                  <div style="background:#0a0a0a;padding:28px 32px;">
                    <h1 style="color:#fff;font-size:22px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">
                      Rich &amp; Retired
                    </h1>
                    <p style="color:#aaa;font-size:12px;margin:6px 0 0;">Order Confirmation</p>
                  </div>

                  <!-- Body -->
                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>, your order has been confirmed! 🎉</p>

                    <!-- Order Meta -->
                    <table style="width:100%%;border-collapse:collapse;margin-bottom:24px;background:#f9f9f9;border-radius:6px;">
                      <tr>
                        <td style="padding:12px 16px;">
                          <span style="font-size:12px;color:#888;display:block;">Order ID</span>
                          <span style="font-size:15px;font-weight:700;color:#0a0a0a;">%s</span>
                        </td>
                        <td style="padding:12px 16px;">
                          <span style="font-size:12px;color:#888;display:block;">Order Date</span>
                          <span style="font-size:14px;color:#333;">%s</span>
                        </td>
                        <td style="padding:12px 16px;">
                          <span style="font-size:12px;color:#888;display:block;">Payment</span>
                          <span style="font-size:14px;color:#333;">%s</span>
                        </td>
                      </tr>
                    </table>

                    <!-- Items Table -->
                    <h3 style="font-size:14px;font-weight:600;color:#555;text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;">
                      Order Items
                    </h3>
                    <table style="width:100%%;border-collapse:collapse;">
                      <thead>
                        <tr style="background:#f0f0f0;">
                          <th style="padding:10px 8px;text-align:left;font-size:12px;color:#888;font-weight:600;">PRODUCT</th>
                          <th style="padding:10px 8px;text-align:center;font-size:12px;color:#888;font-weight:600;">QTY</th>
                          <th style="padding:10px 8px;text-align:right;font-size:12px;color:#888;font-weight:600;">PRICE</th>
                          <th style="padding:10px 8px;text-align:right;font-size:12px;color:#888;font-weight:600;">TOTAL</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>

                    <!-- Pricing Summary -->
                    <table style="width:100%%;border-collapse:collapse;margin-top:16px;">
                      <tr>
                        <td style="padding:6px 8px;color:#555;">Subtotal <span style="font-size:11px;color:#aaa;">(incl. taxes)</span></td>
                        <td style="padding:6px 8px;text-align:right;color:#333;">₹%.2f</td>
                      </tr>
                      <tr>
                        <td style="padding:6px 8px;color:#555;">Shipping</td>
                        <td style="padding:6px 8px;text-align:right;color:#333;">%s</td>
                      </tr>
                      <tr style="border-top:2px solid #0a0a0a;">
                        <td style="padding:12px 8px;font-weight:700;font-size:16px;color:#0a0a0a;">Grand Total</td>
                        <td style="padding:12px 8px;text-align:right;font-weight:700;font-size:16px;color:#0a0a0a;">₹%.2f</td>
                      </tr>
                    </table>

                    <!-- Delivery Address -->
                    <div style="margin-top:24px;padding:16px;background:#f9f9f9;border-radius:6px;">
                      <h3 style="font-size:13px;color:#888;text-transform:uppercase;letter-spacing:1px;margin:0 0 8px;">
                        Delivery Address
                      </h3>
                      <p style="color:#333;font-size:14px;margin:0;">%s</p>
                    </div>

                    <p style="margin-top:28px;font-size:14px;color:#555;">
                      We'll send you another email once your order is <strong>shipped</strong> with tracking details.
                    </p>
                  </div>

                  <!-- Footer -->
                  <div style="background:#f5f5f5;padding:20px 32px;text-align:center;">
                    <p style="color:#888;font-size:12px;margin:0;">
                      Questions? Reply to this email or visit
                      <a href="https://richnretired.com" style="color:#0a0a0a;">richnretired.com</a>
                    </p>
                    <p style="color:#ccc;font-size:11px;margin:8px 0 0;">
                      &copy; 2025 Rich and Retired. All rights reserved.
                    </p>
                  </div>
                </div>
                """.formatted(
                        name, orderId, orderDate, paymentLabel,
                        rows.toString(),
                        subtotal,
                        order.getShippingCharges() != null && order.getShippingCharges() > 0
                                ? "₹" + String.format("%.2f", order.getShippingCharges()) : "FREE",
                        order.getTotalAmount(),
                        address);
    }

    private String buildCancellationHtml(User user, Order order) {

        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        boolean refundPending = order.getPaymentStatus() != null &&
                order.getPaymentStatus().name().equalsIgnoreCase("REFUND_PENDING");

        String refundSection = refundPending ? """
                    <div style="margin-top:20px;padding:16px;background:#fef2f2;border:1px solid #fecaca;border-radius:8px;">
                      <p style="margin:0;font-size:14px;color:#dc2626;font-weight:600;">💳 Refund Processing</p>
                      <p style="margin:6px 0 0;font-size:13px;color:#555;">
                        Your refund of <strong>₹%.2f</strong> is being processed and will be credited to your original payment method within 5–7 business days.
                      </p>
                    </div>
                    """.formatted(order.getTotalAmount()) : "";

        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:580px;margin:auto;background:#fff;">

                  <!-- Header -->
                  <div style="background:#0a0a0a;padding:28px 32px;">
                    <h1 style="color:#fff;font-size:22px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">
                      Rich &amp; Retired
                    </h1>
                    <p style="color:#aaa;font-size:12px;margin:6px 0 0;">Order Cancellation</p>
                  </div>

                  <!-- Body -->
                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;margin-bottom:24px;">
                      Your order <strong>#%d</strong> worth <strong>₹%.2f</strong> has been successfully cancelled.
                    </p>

                    <!-- Status Badge -->
                    <div style="text-align:center;padding:24px;background:#fef2f2;border-radius:12px;margin-bottom:24px;">
                      <div style="font-size:40px;margin-bottom:8px;">❌</div>
                      <div style="display:inline-block;padding:8px 24px;border-radius:20px;background:#dc2626;color:#fff;font-size:14px;font-weight:700;letter-spacing:1px;">
                        ORDER CANCELLED
                      </div>
                    </div>

                    <!-- Order Details -->
                    <table style="width:100%%;background:#f9f9f9;border-radius:8px;margin-bottom:20px;">
                      <tr>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Order ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Order Total</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">₹%.2f</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Status</span>
                          <span style="font-size:14px;font-weight:600;color:#dc2626;">Cancelled</span>
                        </td>
                      </tr>
                    </table>

                    %s

                    <!-- Shop Again CTA -->
                    <div style="text-align:center;margin-top:28px;padding:24px;border:1px dashed #e5e7eb;border-radius:8px;">
                      <p style="color:#555;font-size:14px;margin:0 0 14px;">Looking for something else? Explore our latest collection.</p>
                      <a href="https://richnretired.com/shop"
                         style="display:inline-block;padding:12px 32px;background:#0a0a0a;color:#fff;text-decoration:none;font-size:13px;letter-spacing:2px;text-transform:uppercase;border-radius:6px;">
                        Shop Now
                      </a>
                    </div>

                    <p style="margin-top:24px;font-size:13px;color:#888;">
                      Need help? Contact us at
                      <a href="mailto:support@richnretired.com" style="color:#0a0a0a;">support@richnretired.com</a>
                    </p>
                  </div>

                  <!-- Footer -->
                  <div style="background:#f5f5f5;padding:20px 32px;text-align:center;">
                    <p style="color:#888;font-size:12px;margin:0;">
                      <a href="https://richnretired.com" style="color:#0a0a0a;">richnretired.com</a>
                    </p>
                    <p style="color:#ccc;font-size:11px;margin:8px 0 0;">&copy; 2025 Rich and Retired. All rights reserved.</p>
                  </div>
                </div>
                """.formatted(
                        name, order.getId(), order.getTotalAmount(),
                        order.getId(), order.getTotalAmount(),
                        refundSection);
    }

    private String buildStatusUpdateHtml(User user, Order order, String statusLabel) {

        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        String statusColor = statusColor(order.getStatus().name());
        String statusIcon  = statusIcon(order.getStatus().name());
        String statusMsg   = statusMessage(order.getStatus().name());

        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:560px;margin:auto;background:#fff;">

                  <!-- Header -->
                  <div style="background:#0a0a0a;padding:24px 32px;">
                    <h1 style="color:#fff;font-size:20px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">
                      Rich &amp; Retired
                    </h1>
                  </div>

                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;">
                      Your order <strong>#%d</strong> has been updated.
                    </p>

                    <!-- Status Badge -->
                    <div style="text-align:center;padding:28px 0;">
                      <div style="font-size:48px;">%s</div>
                      <div style="display:inline-block;margin-top:12px;padding:10px 28px;border-radius:24px;background:%s;color:#fff;font-size:16px;font-weight:700;letter-spacing:1px;">
                        %s
                      </div>
                      <p style="color:#555;font-size:14px;margin-top:14px;">%s</p>
                    </div>

                    <!-- Order Info -->
                    <table style="width:100%%;background:#f9f9f9;border-radius:6px;">
                      <tr>
                        <td style="padding:12px 16px;">
                          <span style="font-size:12px;color:#888;display:block;">Order ID</span>
                          <span style="font-size:15px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:12px 16px;">
                          <span style="font-size:12px;color:#888;display:block;">Order Total</span>
                          <span style="font-size:15px;font-weight:700;color:#0a0a0a;">₹%.2f</span>
                        </td>
                      </tr>
                    </table>

                    <p style="margin-top:24px;font-size:13px;color:#888;">
                      Need help? Contact us at support@richnretired.com
                    </p>
                  </div>

                  <!-- Footer -->
                  <div style="background:#f5f5f5;padding:16px 32px;text-align:center;">
                    <p style="color:#ccc;font-size:11px;margin:0;">&copy; 2025 Rich and Retired. All rights reserved.</p>
                  </div>
                </div>
                """.formatted(name, order.getId(), statusIcon, statusColor, statusLabel, statusMsg,
                        order.getId(), order.getTotalAmount());
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    private String friendlyStatus(String status) {
        return switch (status) {
            case "PENDING"           -> "Order Received";
            case "PENDING_PAYMENT"   -> "Awaiting Payment";
            case "PAID"              -> "Payment Confirmed";
            case "PLACED"            -> "Order Placed";
            case "SHIPPED"           -> "Shipped";
            case "DELIVERED"         -> "Delivered";
            case "CANCELLED"         -> "Cancelled";
            case "RETURN_REQUESTED"  -> "Return Requested";
            case "PARTIALLY_CANCELLED" -> "Partially Cancelled";
            default                  -> status;
        };
    }

    private String statusColor(String status) {
        return switch (status) {
            case "DELIVERED"         -> "#16a34a";
            case "SHIPPED"           -> "#2563eb";
            case "PAID", "PLACED"    -> "#7c3aed";
            case "CANCELLED"         -> "#dc2626";
            case "RETURN_REQUESTED"  -> "#d97706";
            default                  -> "#6b7280";
        };
    }

    private String statusIcon(String status) {
        return switch (status) {
            case "DELIVERED"         -> "✅";
            case "SHIPPED"           -> "🚚";
            case "PAID"              -> "💳";
            case "PLACED"            -> "📦";
            case "CANCELLED"         -> "❌";
            case "RETURN_REQUESTED"  -> "🔄";
            default                  -> "📋";
        };
    }

    private String statusMessage(String status) {
        return switch (status) {
            case "SHIPPED"    -> "Your order is on its way! Track it using the link in your account.";
            case "DELIVERED"  -> "Your order has been delivered. We hope you love it! 🎉";
            case "PAID"       -> "Payment received! Your order is being prepared.";
            case "PLACED"     -> "Your order has been confirmed and is being processed.";
            case "CANCELLED"  -> "Your order has been cancelled. Refund (if applicable) will be processed soon.";
            case "RETURN_REQUESTED" -> "Your return request has been received. We'll get back to you shortly.";
            default           -> "Your order status has been updated.";
        };
    }

    private String buildReturnRequestHtml(User user, Return returnRecord) {
        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        String amount = returnRecord.getTotalRefundAmount() != null
                ? "₹" + String.format("%.2f", returnRecord.getTotalRefundAmount())
                : "—";
        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:560px;margin:auto;background:#fff;">
                  <div style="background:#0a0a0a;padding:28px 32px;">
                    <h1 style="color:#fff;font-size:22px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">Rich &amp; Retired</h1>
                    <p style="color:#aaa;font-size:12px;margin:6px 0 0;">Return Request Received</p>
                  </div>
                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;">We've received your return request and it is currently under review.</p>
                    <div style="text-align:center;padding:24px;background:#fefce8;border-radius:12px;margin:24px 0;">
                      <div style="font-size:40px;margin-bottom:8px;">📦</div>
                      <div style="display:inline-block;padding:8px 24px;border-radius:20px;background:#ca8a04;color:#fff;font-size:14px;font-weight:700;letter-spacing:1px;">PENDING REVIEW</div>
                    </div>
                    <table style="width:100%%;background:#f9f9f9;border-radius:8px;margin-bottom:24px;">
                      <tr>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Return ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Order ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Refund Amount</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">%s</span>
                        </td>
                      </tr>
                    </table>
                    <p style="font-size:14px;color:#555;">Our team will review your request within 1–2 business days. Once approved, we'll schedule a pickup from your delivery address.</p>
                    <p style="margin-top:24px;font-size:13px;color:#888;">Need help? Contact us at <a href="mailto:support@richnretired.com" style="color:#0a0a0a;">support@richnretired.com</a></p>
                  </div>
                  <div style="background:#f5f5f5;padding:20px 32px;text-align:center;">
                    <p style="color:#ccc;font-size:11px;margin:0;">&copy; 2025 Rich and Retired. All rights reserved.</p>
                  </div>
                </div>
                """.formatted(name, returnRecord.getId(), returnRecord.getOrder().getId(), amount);
    }

    private String buildReturnApprovedHtml(User user, Return returnRecord) {
        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        String amount = returnRecord.getTotalRefundAmount() != null
                ? "₹" + String.format("%.2f", returnRecord.getTotalRefundAmount())
                : "—";
        String tracking = returnRecord.getTrackingId() != null ? returnRecord.getTrackingId() : "Will be shared soon";
        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:560px;margin:auto;background:#fff;">
                  <div style="background:#0a0a0a;padding:28px 32px;">
                    <h1 style="color:#fff;font-size:22px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">Rich &amp; Retired</h1>
                    <p style="color:#aaa;font-size:12px;margin:6px 0 0;">Return Approved</p>
                  </div>
                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;">Great news! Your return request has been <strong>approved</strong> and pickup has been scheduled. 🎉</p>
                    <div style="text-align:center;padding:24px;background:#f0fdf4;border-radius:12px;margin:24px 0;">
                      <div style="font-size:40px;margin-bottom:8px;">✅</div>
                      <div style="display:inline-block;padding:8px 24px;border-radius:20px;background:#16a34a;color:#fff;font-size:14px;font-weight:700;letter-spacing:1px;">PICKUP SCHEDULED</div>
                    </div>
                    <table style="width:100%%;background:#f9f9f9;border-radius:8px;margin-bottom:24px;">
                      <tr>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Return ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Tracking ID</span>
                          <span style="font-size:14px;font-weight:600;color:#0a0a0a;">%s</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Refund</span>
                          <span style="font-size:16px;font-weight:700;color:#16a34a;">%s</span>
                        </td>
                      </tr>
                    </table>
                    <p style="font-size:14px;color:#555;">Please keep the items ready for pickup. Once we receive and verify the items, your refund of <strong>%s</strong> will be processed within 5–7 business days.</p>
                    <p style="margin-top:24px;font-size:13px;color:#888;">Need help? Contact us at <a href="mailto:support@richnretired.com" style="color:#0a0a0a;">support@richnretired.com</a></p>
                  </div>
                  <div style="background:#f5f5f5;padding:20px 32px;text-align:center;">
                    <p style="color:#ccc;font-size:11px;margin:0;">&copy; 2025 Rich and Retired. All rights reserved.</p>
                  </div>
                </div>
                """.formatted(name, returnRecord.getId(), tracking, amount, amount);
    }

    private String buildReturnRejectedHtml(User user, Return returnRecord, String rejectionReason) {
        String name = (user.getName() != null && !user.getName().isBlank()) ? user.getName() : "Valued Customer";
        String reason = (rejectionReason != null && !rejectionReason.isBlank()) ? rejectionReason : "Does not meet return policy criteria";
        return """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:560px;margin:auto;background:#fff;">
                  <div style="background:#0a0a0a;padding:28px 32px;">
                    <h1 style="color:#fff;font-size:22px;letter-spacing:3px;text-transform:uppercase;font-weight:300;margin:0;">Rich &amp; Retired</h1>
                    <p style="color:#aaa;font-size:12px;margin:6px 0 0;">Return Request Update</p>
                  </div>
                  <div style="padding:32px;">
                    <p style="font-size:16px;color:#333;">Hi <strong>%s</strong>,</p>
                    <p style="font-size:15px;color:#555;">We've reviewed your return request and unfortunately we're unable to process it at this time.</p>
                    <div style="text-align:center;padding:24px;background:#fef2f2;border-radius:12px;margin:24px 0;">
                      <div style="font-size:40px;margin-bottom:8px;">❌</div>
                      <div style="display:inline-block;padding:8px 24px;border-radius:20px;background:#dc2626;color:#fff;font-size:14px;font-weight:700;letter-spacing:1px;">RETURN REJECTED</div>
                    </div>
                    <table style="width:100%%;background:#f9f9f9;border-radius:8px;margin-bottom:24px;">
                      <tr>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Return ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                        <td style="padding:14px 16px;">
                          <span style="font-size:11px;color:#888;display:block;text-transform:uppercase;letter-spacing:1px;">Order ID</span>
                          <span style="font-size:16px;font-weight:700;color:#0a0a0a;">#%d</span>
                        </td>
                      </tr>
                    </table>
                    <div style="padding:16px;background:#fef2f2;border:1px solid #fecaca;border-radius:8px;margin-bottom:24px;">
                      <p style="margin:0;font-size:13px;font-weight:600;color:#dc2626;">Reason for rejection:</p>
                      <p style="margin:6px 0 0;font-size:14px;color:#555;">%s</p>
                    </div>
                    <p style="font-size:14px;color:#555;">If you believe this is an error or have further questions, please contact our support team. We're happy to help.</p>
                    <p style="margin-top:24px;font-size:13px;color:#888;">Need help? Contact us at <a href="mailto:support@richnretired.com" style="color:#0a0a0a;">support@richnretired.com</a></p>
                  </div>
                  <div style="background:#f5f5f5;padding:20px 32px;text-align:center;">
                    <p style="color:#ccc;font-size:11px;margin:0;">&copy; 2025 Rich and Retired. All rights reserved.</p>
                  </div>
                </div>
                """.formatted(name, returnRecord.getId(), returnRecord.getOrder().getId(), reason);
    }

    private void sendEmail(String to, String subject, String htmlBody) throws Exception {
        Email from      = new Email(fromEmail, "Rich & Retired");
        Email recipient = new Email(to);
        Mail mail       = new Mail(from, subject, recipient, new Content("text/html", htmlBody));

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        sg.api(request);
    }
}
