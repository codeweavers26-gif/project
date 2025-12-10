package com.project.backend.entity;

import java.time.LocalDate;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dob;

    private Instant createdAt = Instant.now();
}
