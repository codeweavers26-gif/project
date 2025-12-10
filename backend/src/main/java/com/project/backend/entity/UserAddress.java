package com.project.backend.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_address")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private String addressType; // HOME / OFFICE / OTHER
    private boolean isDefault;

    private Instant createdAt = Instant.now();
}
