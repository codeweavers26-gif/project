package com.project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long>{

}
