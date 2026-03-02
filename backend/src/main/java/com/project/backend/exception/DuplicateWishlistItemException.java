package com.project.backend.exception;

import lombok.Getter;

@Getter
public class DuplicateWishlistItemException extends RuntimeException {
    
    private final Long productId;
    private final Long variantId;
    
    public DuplicateWishlistItemException(Long productId, Long variantId) {
        super(String.format("Item already exists in wishlist - Product: %d, Variant: %d", 
              productId, variantId));
        this.productId = productId;
        this.variantId = variantId;
    }
}