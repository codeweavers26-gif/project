package com.project.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.requestDto.UserAddressDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAddressService {

	private final UserAddressRepository repo;

	public List<UserAddress> getAllAddresses(User user) {
		return repo.findByUser(user);
	}

	public UserAddress addAddress(User user, UserAddressDto dto) {

		if (dto.isDefault()) {
			UserAddress existingDefault = repo.findByUserAndIsDefault(user, true);
			if (existingDefault != null) {
				existingDefault.setDefault(false);
				repo.save(existingDefault);
			}
		}

		UserAddress addr = UserAddress.builder().user(user).addressLine1(dto.getAddressLine1())
				.addressLine2(dto.getAddressLine2()).city(dto.getCity()).state(dto.getState())
				.postalCode(dto.getPostalCode()).country(dto.getCountry()).addressType(dto.getAddressType())
				.isDefault(dto.isDefault()).build();

		return repo.save(addr);
	}

	public void deleteAddress(Long id) {
		repo.deleteById(id);
	}
}
