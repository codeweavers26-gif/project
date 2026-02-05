package com.project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Example: Gurgaon Warehouse

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer deliveryDays;

    @Column(nullable = false)
    private Boolean codAvailable;

    private Double extraShippingCharge;
}
