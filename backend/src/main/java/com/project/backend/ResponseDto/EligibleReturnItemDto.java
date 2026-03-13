package com.project.backend.ResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.backend.entity.ReturnReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleReturnItemDto {
	private Long orderItemId;
	private Long orderId;
	private String productName;
	private String productSku;
	private String productImage;
	private BigDecimal price;
	private Integer quantity;
	private LocalDateTime purchaseDate;
	private Boolean isEligible;
	private String eligibilityMessage;
	private LocalDateTime returnDeadline;
	private List<ReturnReason> availableReasons;
}
