package com.project.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attribute_option")
@Data
@Builder
public class AttributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String value; 

    private Boolean active = true;

    private Integer sortOrder;

    @ManyToOne
    @JoinColumn(name = "attribute_id", nullable = false)
    private AttributeConfig attribute;
}
