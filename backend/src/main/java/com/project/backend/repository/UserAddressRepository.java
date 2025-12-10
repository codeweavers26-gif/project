package com.project.backend.repository;


import java.util.List;

import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser(User user);

    UserAddress findByUserAndIsDefault(User user, boolean isDefault);
}
