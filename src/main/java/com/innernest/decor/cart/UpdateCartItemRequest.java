package com.innernest.decor.cart;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(@Positive int qty) {
}
