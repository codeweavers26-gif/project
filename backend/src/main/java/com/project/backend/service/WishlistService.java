package com.project.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.project.backend.ResponseDto.WishlistResponseDto;
import com.project.backend.entity.Product;
import com.project.backend.entity.User;
import com.project.backend.entity.Wishlist;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.ProductRepository;
import com.project.backend.repository.WishlistRepository;
import com.project.backend.requestDto.PageResponseDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;

    public void add(User user, Long productId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (wishlistRepo.existsByUserAndProduct(user, product)) {
            return; // idempotent
        }

        wishlistRepo.save(
            Wishlist.builder()
                .user(user)
                .product(product)
                .build()
        );
    }

    @Transactional   // 🔴 THIS WAS MISSING
    public void remove(User user, Long productId) {

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Wishlist wishlist = wishlistRepo
                .findByUserAndProduct(user, product)
                .orElseThrow(() -> new NotFoundException("Item not in wishlist"));

        wishlistRepo.delete(wishlist);
    }

    public PageResponseDto<WishlistResponseDto> get(User user, int page, int size) {

        Page<Wishlist> data =
            wishlistRepo.findByUser(user, PageRequest.of(page, size));

        return PageResponseDto.<WishlistResponseDto>builder()
                .content(
                    data.getContent().stream().map(w ->
                        WishlistResponseDto.builder()
                            .productId(w.getProduct().getId())
                            .productName(w.getProduct().getName())
                            .price(w.getProduct().getPrice())
                            .inStock(w.getProduct().getStock() > 0)
                            .build()
                    ).toList()
                )
                .page(data.getNumber())
                .size(data.getSize())
                .totalElements(data.getTotalElements())
                .totalPages(data.getTotalPages())
                .last(data.isLast())
                .build();
    }
}
