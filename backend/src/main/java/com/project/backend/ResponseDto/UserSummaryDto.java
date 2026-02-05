package com.project.backend.ResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDto {
    private Long totalUsers;
    private Long usersLast30Days;
    private Long usersLast7Days;
}
