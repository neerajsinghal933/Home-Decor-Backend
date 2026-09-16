package com.innernest.decor.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(@NotNull Long productId, @Positive int qty, String size, String color) {
}
