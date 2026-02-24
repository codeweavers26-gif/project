package com.project.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "warehouses")
public class Warehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(unique = true, length = 50)
	private String code; // Warehouse code like "DEL-01", "MUM-01"

	@Column(name = "location_id")
	private Long locationId; // 🔥 ADD THIS - Reference to your location table

	private String address;
	private String city;
	private String state;
	private String country;
	private String pincode;

	@Column(name = "contact_person")
	private String contactPerson;

	@Column(name = "contact_phone")
	private String contactPhone;

	@Column(name = "contact_email")
	private String contactEmail;

	@Column(name = "is_default")
	private Boolean isDefault = false;

	@Column(name = "is_active")
	private Boolean isActive = true;

	// Geo coordinates for future use
	private Double latitude;
	private Double longitude;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<WarehouseInventory> inventories;

	@OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
	private List<Order> orders;

	@OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
	private List<Shipment> shipments;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}