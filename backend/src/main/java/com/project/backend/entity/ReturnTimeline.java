package com.project.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "return_timeline", 
       indexes = {
           @Index(name = "idx_timeline_return_id", columnList = "return_id"),
           @Index(name = "idx_timeline_created_at", columnList = "created_at"),
           @Index(name = "idx_timeline_status", columnList = "status")
       })
public class ReturnTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private Return returnRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ReturnStatus status;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy; 

    @Column(name = "created_by_role", length = 20)
    private String createdByRole;  

    @Column(name = "is_customer_visible")
    private Boolean isCustomerVisible = true;

    @Column(name = "is_admin_visible")
    private Boolean isAdminVisible = true;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;  

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isCustomerVisible == null) {
            isCustomerVisible = true;
        }
        if (isAdminVisible == null) {
            isAdminVisible = true;
        }
    }
}