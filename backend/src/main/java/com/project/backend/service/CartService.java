package com.project.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.ResponseDto.CartItemResponseDto;
import com.project.backend.ResponseDto.CartPricingResponseDto;
import com.project.backend.entity.Cart;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;
import com.project.backend.exception.BadRequestException;
import com.project.backend.exception.NotFoundException;
import com.project.backend.repository.CartRepository;
import com.project.backend.repository.ProductRepository;
import com.project.backend.requestDto.CartMergeDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final ProductRepository productRepository;

	@Transactional
	public void addToCart(User user, Long productId, Integer qty) {

		if (qty == null || qty <= 0) {
			throw new BadRequestException("Quantity must be greater than 0");
		}

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

		if (!Boolean.TRUE.equals(product.getIsActive())) {
			throw new BadRequestException("Product is not available: " + product.getName());
		}

		// 🔥 FIX: Use getTotalStock() instead of getStock()
		Integer availableStock = product.getTotalStock();
		if (availableStock < qty) {
			throw new BadRequestException(
					String.format("Only %d units available for %s", availableStock, product.getName()));
		}

		Cart cart = cartRepository.findByUserAndProduct(user, product)
				.orElse(Cart.builder().user(user).product(product).quantity(0).build());

		int newQuantity = cart.getQuantity() + qty;

		// 🔥 FIX: Use getTotalStock() for validation
		if (availableStock < newQuantity) {
			throw new BadRequestException(String.format("Cannot add %d more. Only %d available in stock", qty,
					availableStock - cart.getQuantity()));
		}

		cart.setQuantity(newQuantity);
		cartRepository.save(cart);

		log.info("Added to cart - User: {}, Product: {}, Quantity: {}, New Total: {}", user.getId(), productId, qty,
				newQuantity);
	}

	// VIEW CART
	public List<CartItemResponseDto> getCart(User user) {

		List<Cart> cartItems = cartRepository.findByUser(user);

		if (cartItems.isEmpty()) {
			return new ArrayList<>(); 
		}

		return cartItems.stream().map(cart -> {
			Product product = cart.getProduct();
			Double price = product.getMinPrice() != null ? product.getMinPrice() : 0.0;
			Integer quantity = cart.getQuantity() != null ? cart.getQuantity() : 0;

			return CartItemResponseDto.builder().cartId(cart.getId()).productId(product.getId())
					.productName(product.getName() != null ? product.getName() : "Unknown")
					.imageUrl(getPrimaryImage(product)).price(price).quantity(quantity).totalPrice(price * quantity)
					.build();
		}).collect(Collectors.toList());
	}

	// GET PRIMARY IMAGE
	private String getPrimaryImage(Product product) {
		if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
			return null;
		}

		return product.getImages().stream().filter(img -> img.getIsPrimary() != null && img.getIsPrimary()).findFirst()
				.map(ProductImage::getImageUrl).orElse(product.getImages().get(0).getImageUrl());
	}

	@Transactional
	public void updateQuantity(User user, Long cartId, Integer qty) {

		if (qty == null) {
			throw new BadRequestException("Quantity is required");
		}

		Cart cart = cartRepository.findById(cartId)
				.orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartId));

		if (!cart.getUser().getId().equals(user.getId())) {
			throw new BadRequestException("Unauthorized to modify this cart item");
		}

		if (qty <= 0) {
			cartRepository.delete(cart);
			log.info("Removed cart item - User: {}, CartItem: {}", user.getId(), cartId);
		} else {
			Product product = cart.getProduct();
			// 🔥 FIX: Use getTotalStock() instead of getStock()
			Integer availableStock = product.getTotalStock();

			if (availableStock < qty) {
				throw new BadRequestException(
						String.format("Only %d units available for %s", availableStock, product.getName()));
			}

			cart.setQuantity(qty);
			cartRepository.save(cart);
			log.info("Updated cart item - User: {}, CartItem: {}, New Quantity: {}", user.getId(), cartId, qty);
		}
	}

	// REMOVE ITEM
	@Transactional
	public void removeItem(User user, Long cartId) {

		Cart cart = cartRepository.findById(cartId)
				.orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartId));

		if (!cart.getUser().getId().equals(user.getId())) {
			throw new BadRequestException("Unauthorized to remove this cart item");
		}

		cartRepository.delete(cart);
		log.info("Removed cart item - User: {}, CartItem: {}", user.getId(), cartId);
	}

	// CLEAR CART (after checkout)
	@Transactional
	public void clearCart(User user) {
		cartRepository.deleteByUser(user);
		log.info("Cleared cart for user: {}", user.getId());
	}

	// MERGE CART (for guest to user migration)
	@Transactional
	public void mergeCart(User user, List<CartMergeDto> items) {
		if (items == null || items.isEmpty()) {
			return;
		}

		for (CartMergeDto item : items) {
			try {
				addToCart(user, item.getProductId(), item.getQuantity());
			} catch (Exception e) {
				log.warn("Failed to merge item - Product: {}, Error: {}", item.getProductId(), e.getMessage());
				// Continue with other items instead of failing completely
			}
		}
		log.info("Merged {} items to cart for user: {}", items.size(), user.getId());
	}

	// GET CART PRICING WITH CALCULATIONS
	public CartPricingResponseDto getCartPricing(User user, String couponCode) {

		List<Cart> cartItems = cartRepository.findByUser(user);

		// 🔥 FIX: Empty cart response
		if (cartItems == null || cartItems.isEmpty()) {
			return CartPricingResponseDto.builder().items(new ArrayList<>()).subtotal(0.0).taxAmount(0.0)
					.shippingCharges(0.0).discountAmount(0.0).finalAmount(0.0).couponApplied(false)
					.message("Your cart is empty").build();
		}

		double subtotal = 0.0;
		double tax = 0.0;
		List<CartItemResponseDto> itemDtos = new ArrayList<>();

		for (Cart cartItem : cartItems) {
			Product product = cartItem.getProduct();

			// 🔥 FIX: Validate product exists
			if (product == null) {
				log.warn("Cart item {} has null product, skipping", cartItem.getId());
				continue;
			}

			int qty = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;

			// 🔥 FIX: Check if product is active
			if (!Boolean.TRUE.equals(product.getIsActive())) {
				throw new BadRequestException(product.getName() + " is not available");
			}

			Double price = product.getMinPrice() != null ? product.getMinPrice() : 0.0;
			double itemTotal = price * qty;

			// 🔥 FIX: Tax calculation
			double taxPercent = product.getTaxPercent() != null ? product.getTaxPercent() : 0.0;
			double itemTax = (taxPercent / 100.0) * itemTotal;

			subtotal += itemTotal;
			tax += itemTax;

			itemDtos.add(CartItemResponseDto.builder().cartId(cartItem.getId()).productId(product.getId())
					.productName(product.getName()).imageUrl(getPrimaryImage(product)).price(price).quantity(qty)
					.totalPrice(itemTotal).build());
		}

		// 🔥 FIX: Shipping logic with proper rounding
		double shipping = subtotal > 999 ? 0.0 : 49.0;

		// 🎟 Coupon Logic (commented out for now)
		double discount = 0.0;
		boolean couponApplied = false;
		String message = null;

		double finalAmount = subtotal + tax + shipping - discount;

		// 🔥 FIX: Round to 2 decimal places
		return CartPricingResponseDto.builder().items(itemDtos).subtotal(Math.round(subtotal * 100.0) / 100.0)
				.taxAmount(Math.round(tax * 100.0) / 100.0).shippingCharges(shipping).discountAmount(discount)
				.finalAmount(Math.round(finalAmount * 100.0) / 100.0).appliedCoupon(couponApplied ? couponCode : null)
				.couponApplied(couponApplied).message(message).build();
	}

	// ADD OR UPDATE CART ITEM (helper method)
	@Transactional
	public void addOrUpdate(User user, Product product, int quantity) {

		// 🔥 FIX: Validate inputs
		if (product == null) {
			throw new BadRequestException("Product cannot be null");
		}

		Cart cart = cartRepository.findByUserAndProduct(user, product)
				.orElse(Cart.builder().user(user).product(product).quantity(0).build());

		int newQuantity = cart.getQuantity() + quantity;

		Integer availableStock = 1000;
		if (availableStock < newQuantity) {
			throw new BadRequestException(
					String.format("Only %d units available for %s", availableStock, product.getName()));
		}

		cart.setQuantity(newQuantity);

		if (cart.getQuantity() <= 0) {
			cartRepository.delete(cart);
			log.info("Removed cart item via addOrUpdate - User: {}, Product: {}", user.getId(), product.getId());
		} else {
			cartRepository.save(cart);
			log.info("Updated cart via addOrUpdate - User: {}, Product: {}, Quantity: {}", user.getId(),
					product.getId(), newQuantity);
		}
	}
}