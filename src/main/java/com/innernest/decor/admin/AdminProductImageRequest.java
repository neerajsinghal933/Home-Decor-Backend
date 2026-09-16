package com.innernest.decor.admin;

import jakarta.validation.constraints.Size;

public record AdminProductImageRequest(
    @Size(max = 500) String url,
    @Size(max = 80) String color) {
}
