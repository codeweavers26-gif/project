package com.project.backend.ResponseDto;

import com.project.backend.entity.PaymentTransaction;
import com.project.backend.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDetailDto {
    private RefundDto refundInfo;
    private PaymentTransaction paymentDetails;
    private ReturnDto returnDetails;
    private User processedByUser;
}