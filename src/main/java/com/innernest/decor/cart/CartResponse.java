package com.innernest.decor.cart;

import java.util.List;

public record CartResponse(List<CartItemResponse> items, CartTotalsResponse totals) {
}
