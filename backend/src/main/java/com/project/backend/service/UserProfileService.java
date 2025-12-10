package com.project.backend.service;

import org.springframework.stereotype.Service;

import com.project.backend.entity.User;
import com.project.backend.entity.UserProfile;
import com.project.backend.repository.UserProfileRepository;
import com.project.backend.requestDto.UserProfileDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfile getProfile(User user) {
        return userProfileRepository.findByUser(user).orElse(null);
    }

    public UserProfile updateProfile(User user, UserProfileDto dto) {
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(UserProfile.builder().user(user).build());

        profile.setFullName(dto.getFullName());
        profile.setPhone(dto.getPhone());
        profile.setGender(dto.getGender());
        profile.setDob(dto.getDob() != null ? java.time.LocalDate.parse(dto.getDob()) : null);

        return userProfileRepository.save(profile);
    }
}
