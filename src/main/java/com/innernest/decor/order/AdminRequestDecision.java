package com.innernest.decor.order;

import jakarta.validation.constraints.Size;

public record AdminRequestDecision(@Size(max = 1000) String note) {
}
