package com.project.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CartItemResponseDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.CartMergeDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    // ADD TO CART
    @Transactional
    public void addToCart(User user, Long productId, Integer qty) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .orElse(Cart.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .build());

        cart.setQuantity(cart.getQuantity() + qty);

        cartRepository.save(cart);
    }

    // VIEW CART
    public List<CartItemResponseDto> getCart(User user) {

        return cartRepository.findByUser(user)
                .stream()
                .map(cart -> CartItemResponseDto.builder()
                        .cartId(cart.getId())
                        .productId(cart.getProduct().getId())
                        .productName(cart.getProduct().getName())
                        .imageUrl(getPrimaryImage(cart.getProduct()))
                        .price(cart.getProduct().getPrice())
                        .quantity(cart.getQuantity())
                        .totalPrice(cart.getQuantity() * cart.getProduct().getPrice())
                        .build())
                .collect(Collectors.toList());
    }


    private String getPrimaryImage(Product product) {
        return product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                    .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(null)
                : null;
    }

    // UPDATE QUANTITY
    public void updateQuantity(User user, Long cartId, Integer qty) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (qty <= 0) {
            cartRepository.delete(cart);
        } else {
            cart.setQuantity(qty);
            cartRepository.save(cart);
        }
    }

    // REMOVE ITEM
    public void removeItem(User user, Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        cartRepository.delete(cart);
    }

    // CLEAR CART (after checkout)
    public void clearCart(User user) {
        cartRepository.deleteByUser(user);
    }
    @Transactional
    public void mergeCart(User user, List<CartMergeDto> items) {

        for (CartMergeDto item : items) {
            addToCart(user, item.getProductId(), item.getQuantity());
        }
    }

}
