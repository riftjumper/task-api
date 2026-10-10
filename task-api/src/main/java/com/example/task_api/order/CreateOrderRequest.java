package com.example.task_api.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @JsonProperty("product_id") @NotNull @Positive Long productId,
        @NotNull @Positive @Digits(integer = 9, fraction = 0) BigDecimal quantity
) {
}
