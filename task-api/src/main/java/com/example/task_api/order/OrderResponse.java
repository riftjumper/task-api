package com.example.task_api.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        @JsonProperty("product_id") Long productId,
        int quantity,
        @JsonProperty("total_price") BigDecimal totalPrice,
        String status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name()
        );
    }
}
