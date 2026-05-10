
	package com.project.backend.mapper;

	import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.project.backend.ResponseDto.OrderItemResponseDto;
import com.project.backend.ResponseDto.OrderResponseDto;
import com.project.backend.ResponseDto.OrderStatusHistoryDto;
import com.project.backend.entity.Order;
import com.project.backend.entity.OrderItem;
import com.project.backend.entity.OrderStatusHistory;
import com.project.backend.entity.Product;
import com.project.backend.entity.ProductImage;
import com.project.backend.entity.User;

	public class OrderMapper {

		/** Full: images + status history. */
		public static OrderResponseDto toDto(Order order, Map<Long, String> imageMap, List<OrderStatusHistory> history) {

		    User user = order.getUser();

		    double subtotal = order.getItems() != null
		            ? order.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum()
		            : 0.0;

		    List<OrderStatusHistoryDto> historyDtos = history != null
		            ? history.stream()
		                .sorted(Comparator.comparing(OrderStatusHistory::getChangedAt))
		                .map(h -> OrderStatusHistoryDto.builder()
		                        .status(h.getStatus())
		                        .changedAt(h.getChangedAt())
		                        .build())
		                .toList()
		            : new ArrayList<>();

		    return OrderResponseDto.builder()

		            .orderId(order.getId())

		            .totalAmount(order.getTotalAmount())
		            .subtotal(subtotal)
		            .taxAmount(order.getTaxAmount())
		            .shippingCharges(order.getShippingCharges())
		            .discountAmount(order.getDiscountAmount())
		            .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
		            .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
		            .status(order.getStatus())
		            .createdAt(order.getCreatedAt())

		            .userId(user != null ? user.getId() : null)
		            .userName(user != null ? user.getName() : null)
		            .userEmail(user != null ? user.getEmail() : null)

		            .deliveryAddressLine1(order.getDeliveryAddressLine1())
		            .deliveryAddressLine2(order.getDeliveryAddressLine2())
		            .deliveryCity(order.getDeliveryCity())
		            .deliveryState(order.getDeliveryState())
		            .deliveryPostalCode(order.getDeliveryPostalCode())
		            .deliveryCountry(order.getDeliveryCountry())

		            .items(order.getItems() != null
		                    ? order.getItems().stream()
		                        .map(item -> mapItemToResponse(item, imageMap))
		                        .toList()
		                    : new ArrayList<>())

		            .statusHistory(historyDtos)

		            .build();
		}

		/** Images only, no history. */
		public static OrderResponseDto toDto(Order order, Map<Long, String> imageMap) {
		    return toDto(order, imageMap, Collections.emptyList());
		}

		/** Fallback: no images, no history. */
		public static OrderResponseDto toDto(Order order) {
		    return toDto(order, Collections.emptyMap(), Collections.emptyList());
		}

		private static OrderItemResponseDto mapItemToResponse(OrderItem item, Map<Long, String> imageMap) {
		    String imageUrl = (imageMap != null && item.getProductId() != null)
		            ? imageMap.get(item.getProductId()) : null;

		    return OrderItemResponseDto.builder()
			.orderItemId(item.getId())
		            .productId(item.getProductId())
		            .variantId(item.getVariantId())
		            .productName(item.getProductName())
		            .imageUrl(imageUrl)
		            .price(item.getPrice())
		            .quantity(item.getQuantity())
		            .totalPrice(
		                    BigDecimal.valueOf(item.getPrice())
		                            .multiply(BigDecimal.valueOf(item.getQuantity()))
		            )
		            .build();
		}
	    
	    private static String getPrimaryImage(Product product) {
	        return product.getImages() != null && !product.getImages().isEmpty()
	                ? product.getImages().stream()
	                    .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
	                    .findFirst()
	                    .map(ProductImage::getImageUrl)
	                    .orElse(null)
	                : null;
	    }

	}