package com.innernest.decor.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 40) String phone,
    @Size(max = 500) String profileImageUrl) {
}
