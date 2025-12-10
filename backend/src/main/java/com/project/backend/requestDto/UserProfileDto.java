package com.project.backend.requestDto;


import lombok.Data;

@Data
public class UserProfileDto {
    private String fullName;
    private String phone;
    private String gender;
    private String dob; // yyyy-mm-dd
}
