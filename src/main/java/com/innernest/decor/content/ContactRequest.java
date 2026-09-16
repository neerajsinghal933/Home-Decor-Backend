package com.innernest.decor.content;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(@NotBlank @Size(max = 140) String name, @NotBlank @Email String email, @NotBlank @Size(max = 4000) String message) {
}
