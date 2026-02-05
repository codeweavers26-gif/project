package com.project.backend.service;

import com.project.backend.ResponseDto.UserResponseDto;
import com.project.backend.ResponseDto.UserSummaryDto;
import com.project.backend.entity.User;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<UserResponseDto> getUsers(String search, String fromDate, String toDate, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Instant from = fromDate != null ? LocalDate.parse(fromDate).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant to = toDate != null ? LocalDate.parse(toDate).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

        Page<User> users = userRepository.searchUsers(search, from, to, pageable);

        return users.map(user -> UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build());
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserSummaryDto getUserSummary() {
        Instant now = Instant.now();

        Instant last30Days = now.minusSeconds(30L * 24 * 60 * 60);
        Instant last7Days = now.minusSeconds(7L * 24 * 60 * 60);

        return new UserSummaryDto(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(last30Days),
                userRepository.countByCreatedAtAfter(last7Days)
        );
    }
}
