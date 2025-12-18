package com.project.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.backend.entity.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location,Long>{
	  boolean existsByCityAndPincode(String city, String pincode);
	  List<Location> findByIsActiveTrue();
}
