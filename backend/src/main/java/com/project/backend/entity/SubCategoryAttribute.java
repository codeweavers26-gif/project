package com.project.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "subcategory_attribute")
@Data
@Builder
public class SubCategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subcategory_id", nullable = false)
    private SubCategory subCategory;

    @ManyToOne
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeConfig attribute;

    private Boolean required = false;
    private Boolean filterable = true;
}
