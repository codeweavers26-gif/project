package com.project.backend.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRefundRequest {
	@NotBlank(message = "Transaction ID is required")
	private String transactionId;

	private String gatewayResponse;
	private String notes;
}
