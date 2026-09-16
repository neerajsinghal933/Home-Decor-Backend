package com.innernest.decor.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
  private MoneyUtils() {
  }

  public static BigDecimal rupees(Number value) {
    return BigDecimal.valueOf(value.longValue()).setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal tax(BigDecimal subtotal) {
    return subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(0, RoundingMode.HALF_UP).setScale(2);
  }
}
