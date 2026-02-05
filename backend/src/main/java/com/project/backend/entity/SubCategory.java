package com.project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subcategories")
@Data
@Builder
@NoArgsConstructor   // ✅ REQUIRED by JPA
@AllArgsConstructor  // ✅ Needed for Builder
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
