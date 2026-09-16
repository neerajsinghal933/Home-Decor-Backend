package com.innernest.decor.admin;

import com.innernest.decor.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusRequest(@NotNull OrderStatus status) {
}

