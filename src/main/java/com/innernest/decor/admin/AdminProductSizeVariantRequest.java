package com.innernest.decor.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminProductSizeVariantRequest(
    @NotBlank @Size(max = 60) String size,
    @NotNull @DecimalMin("0.01") BigDecimal price) {
}
