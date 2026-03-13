package com.project.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.entity.User;
import com.project.backend.entity.UserAddress;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.UserAddressRepository;
import com.project.backend.requestDto.UserAddressDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAddressService {

	private final UserAddressRepository repo;

	public List<UserAddress> getAllAddresses(User user) {
		return repo.findByUserAndIsActiveTrue(user);
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
				.postalCode(dto.getPostalCode()).country(dto.getCountry()).addressType(dto.getAddressType()).isActive(true)
				.isDefault(dto.isDefault()).build();

		return repo.save(addr);
	}

	 @Transactional
	    public void deleteAddress(Long id) {
	        UserAddress address = repo.findById(id)
	            .orElseThrow(() -> new NotFoundException("Address not found with id: " + id));
	        
	        address.setIsActive(false);  
	        
	        repo.save(address); 
	    }
	
	@Transactional
	public UserAddress updateAddress(User user, Long addressId, UserAddressDto dto) {
	    
	    UserAddress address = repo.findById(addressId)
	            .orElseThrow(() -> new NotFoundException("Address not found with id: " + addressId));
	    
	  
	    
	    if (dto.isDefault() && !address.isDefault()) {
	        UserAddress existingDefault = repo.findByUserAndIsDefault(user, true);
	        if (existingDefault != null && !existingDefault.getId().equals(addressId)) {
	            existingDefault.setDefault(false);
	            repo.save(existingDefault);
	        }
	    }
	    
	    address.setAddressLine1(dto.getAddressLine1());
	    address.setAddressLine2(dto.getAddressLine2());
	    address.setCity(dto.getCity());
	    address.setIsActive(true);
	    address.setState(dto.getState());
	    address.setPostalCode(dto.getPostalCode());
	    address.setCountry(dto.getCountry());
	    address.setAddressType(dto.getAddressType());
	    address.setDefault(dto.isDefault());
	    
	    return repo.save(address);
	}
}
