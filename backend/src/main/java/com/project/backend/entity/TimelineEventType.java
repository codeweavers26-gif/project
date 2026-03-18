package com.project.backend.entity;


public enum TimelineEventType {
    RETURN_REQUESTED("Return request submitted"),
    RETURN_REQUEST_UPDATED("Return request updated"),
    RETURN_CANCELLED("Return request cancelled by customer"),
    
    RETURN_UNDER_REVIEW("Return under admin review"),
    RETURN_APPROVED("Return request approved"),
    RETURN_REJECTED("Return request rejected"),
    RETURN_REJECTION_REASON("Rejection reason provided"),
    
    PICKUP_SCHEDULED("Pickup scheduled"),
    PICKUP_RESCHEDULED("Pickup rescheduled"),
    PICKUP_AGENT_ASSIGNED("Pickup agent assigned"),
    PICKUP_OUT_FOR_PICKUP("Agent out for pickup"),
    PICKUP_COMPLETED("Item picked up successfully"),
    PICKUP_FAILED("Pickup attempt failed"),
    PICKUP_RETRY_SCHEDULED("Pickup retry scheduled"),
    
    QC_PENDING("Awaiting quality check"),
    QC_IN_PROGRESS("Quality check in progress"),
    QC_PASSED("Quality check passed"),
    QC_FAILED("Quality check failed"),
    QC_ISSUES_FOUND("Issues found during quality check"),
    
    REFUND_INITIATED("Refund process initiated"),
    REFUND_PROCESSING("Refund being processed"),
    REFUND_COMPLETED("Refund completed successfully"),
    REFUND_FAILED("Refund failed"),
    REFUND_RETRY_SCHEDULED("Refund retry scheduled"),
    
    CUSTOMER_NOTIFIED("Customer notified"),
    ADMIN_COMMENT_ADDED("Admin comment added"),
    CUSTOMER_MESSAGE_ADDED("Customer message added"),
    SUPPORT_TICKET_CREATED("Support ticket created"),
    
    SYSTEM_AUTO_APPROVED("Auto-approved by system"),
    SYSTEM_ESCALATED("Escalated to admin"),
    DEADLINE_REMINDER("Deadline reminder sent"),
    RETURN_AUTO_CLOSED("Return auto-closed by system"),
    
    IMAGES_UPLOADED("Images uploaded"),
    INVOICE_GENERATED("Refund invoice generated"),
    RECEIPT_GENERATED("Receipt generated"),
    
    ERROR_OCCURRED("Error occurred"),
    VALIDATION_FAILED("Validation failed");

    private final String defaultDescription;

    TimelineEventType(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }
}