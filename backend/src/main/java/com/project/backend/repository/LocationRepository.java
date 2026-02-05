package com.project.backend.repository;

import com.project.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByIsActiveTrue();

    Optional<Location> findFirstByPincodeAndIsActiveTrue(String pincode);

    boolean existsByCityAndPincode(String city, String pincode);
}
