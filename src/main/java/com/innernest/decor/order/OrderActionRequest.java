package com.innernest.decor.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderActionRequest(
    @NotBlank @Size(max = 300) String reason,
    @Size(max = 1000) String details) {
}
