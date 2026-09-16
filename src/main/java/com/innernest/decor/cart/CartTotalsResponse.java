package com.innernest.decor.cart;

import java.math.BigDecimal;

public record CartTotalsResponse(int itemCount, BigDecimal subtotal, BigDecimal shipping, BigDecimal estimatedTax, BigDecimal total) {
}
