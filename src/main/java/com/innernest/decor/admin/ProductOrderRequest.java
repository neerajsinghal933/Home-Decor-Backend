package com.innernest.decor.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductOrderRequest(@NotNull Long id, @Min(0) int displayOrder) {
}
