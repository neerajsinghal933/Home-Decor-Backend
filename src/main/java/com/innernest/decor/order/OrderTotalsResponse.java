package com.innernest.decor.order;

import java.math.BigDecimal;

public record OrderTotalsResponse(int itemCount, BigDecimal subtotal, BigDecimal shipping, BigDecimal estimatedTax, BigDecimal discount, BigDecimal total) {
}
