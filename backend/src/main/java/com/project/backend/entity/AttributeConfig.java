package com.project.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "attribute_config")
@Data
public class AttributeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // color, size

    private Boolean required;

    private String inputType; // TEXT or SELECT

    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "category")
    private Category category;
}


