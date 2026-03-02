package com.project.backend.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistRequestDto {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long variantId;
  
	public Integer quantity;
}