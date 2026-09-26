package com.innernest.decor.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record AdminProductRequest(
    @NotBlank @Size(max = 80) String sku,
    @NotBlank @Size(max = 160) String slug,
    @NotBlank @Size(max = 180) String name,
    @Size(max = 5000) String description,
    @NotBlank @Size(max = 120) String categoryId,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @DecimalMin("0.01") BigDecimal old,
    @DecimalMin("0.0") BigDecimal rating,
    @Size(max = 80) String badge,
    @Size(max = 80) String color,
    @Size(max = 120) String material,
    @Size(max = 160) String dimensions,
    @Min(0) int stock,
    @Min(0) int reviews,
    boolean featured,
    @Min(0) int displayOrder,
    Boolean active,
    @Size(max = 255) String image,
    List<@Size(max = 500) String> images,
    List<AdminProductImageRequest> colorImages,
    @Size(max = 30) List<@NotNull @Valid AdminProductSizeVariantRequest> sizeVariants,
    List<Long> tagIds) {
}
