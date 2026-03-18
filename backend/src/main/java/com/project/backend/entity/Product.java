package com.project.backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
@NamedEntityGraph(
    name = "product-with-details",
    attributeNodes = {
        @NamedAttributeNode("category"),
        @NamedAttributeNode("variants"),
        @NamedAttributeNode("images")
    }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(length = 20)
    private String status = "ACTIVE";
    private String slug;
    private String sku; 
    private Double mrp; 
    @Column(name = "discount_percent")
    private Double discountPercent;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    @Column(name = "cod_available")
    private Boolean codAvailable = true;
    
    private Boolean returnable = true;
    @Column(name = "average_rating")
    private Double averageRating = 0.0;
    
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;
    @Column(name = "delivery_days")
    private Integer deliveryDays;
    private String brand;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private Double price;

    @Column(name = "tax_percent")
    private Double taxPercent;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Integer stock = 0;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProductVariant> variants = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Product(Long id, String name, String slug, String sku, String brand, String shortDescription, 
                  String description, Category category, Double price,Double mrp, Double discountPercent,
                  Double taxPercent, Double weight, Double length, 
                  Double width, Double height, Boolean codAvailable, Boolean returnable,
                  Integer deliveryDays, Double averageRating, Integer totalReviews,
                  Boolean isActive, Boolean isDeleted, LocalDateTime createdAt, 
                  LocalDateTime updatedAt, List<ProductVariant> variants, List<ProductImage> images, Integer stock) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.brand = brand;
        this.shortDescription = shortDescription;
        this.description = description;
        this.category = category;
        this.sku = sku;
        this.mrp = mrp;
        this.discountPercent = discountPercent;
        this.taxPercent = taxPercent;
        this.stock = stock != null ? stock : 0;
        this.weight = weight;
        this.length = length;
        this.width = width;
        this.height = height;
        this.codAvailable = codAvailable != null ? codAvailable : true;
        this.returnable = returnable != null ? returnable : true;
        this.deliveryDays = deliveryDays;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.totalReviews = totalReviews != null ? totalReviews : 0;
        this.price = price;
        this.taxPercent = taxPercent;
        this.isActive = isActive != null ? isActive : true;
        this.isDeleted = isDeleted != null ? isDeleted : false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.variants = variants != null ? variants : new ArrayList<>();
        this.images = images != null ? images : new ArrayList<>();
        this.stock = stock;
    }
    
   
    
    public boolean hasStock() {
        return getTotalStock() > 0;
    }
    
    public Double getMinPrice() {
        if (variants == null || variants.isEmpty()) {
            return price != null ? price : 0.0;
        }
        
        return variants.stream()
            .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
            .map(v -> v.getSellingPrice() != null ? v.getSellingPrice().doubleValue() : Double.MAX_VALUE)
            .min(Double::compareTo)
            .orElse(price != null ? price : 0.0);
    }
    
    public Double getMaxPrice() {
        if (variants == null || variants.isEmpty()) {
            return price != null ? price : 0.0;
        }
        
        return variants.stream()
            .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
            .map(v -> v.getSellingPrice() != null ? v.getSellingPrice().doubleValue() : 0.0)
            .max(Double::compareTo)
            .orElse(price != null ? price : 0.0);
    }
    public Integer getTotalStock() {
        if (variants == null || variants.isEmpty()) {
            return 0;
        }
        
        return variants.stream()
            .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
            .mapToInt(v -> {
                if (v.getInventories() == null || v.getInventories().isEmpty()) {
                    return 0;
                }
                return v.getInventories().stream()
                    .mapToInt(wi -> wi.getAvailableQuantity() != null ? wi.getAvailableQuantity() : 0)
                    .sum();
            })
            .sum();
    }

   
}