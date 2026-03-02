package com.project.backend.requestDto;


import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveToCartRequestDto {
    
    @NotEmpty(message = "At least one item must be selected")
    private List<Long> itemIds; 
    
    private Boolean removeFromWishlist = true;
}