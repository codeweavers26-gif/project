package com.project.backend.entity;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;

    @Column(unique = true)
    private String sku;

    @Column(unique = true)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double mrp;

    @Column(nullable = false)
    private Double price;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "tax_percent")
    private Double taxPercent;

    @Column(nullable = false)
    private Integer stock;

    private Double weight;
    private Double length;
    private Double width;
    private Double height;

    @Column(name = "cod_available")
    private Boolean codAvailable;

    private Boolean returnable;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    private String imagePublicId;      // Cloudinary public ID (for deletion)
    private String imageUrl;           // Main image URL
    private String thumbnailUrl;       // Thumbnail URL
    private String mediumImageUrl;      // Medium size URL
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttribute> attributes;
}
