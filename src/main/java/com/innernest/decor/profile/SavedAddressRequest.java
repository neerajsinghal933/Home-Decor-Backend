package com.innernest.decor.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedAddressRequest(
    @NotBlank @Size(max = 120) String fullName,
    @Size(max = 40) String phone,
    @NotBlank @Size(max = 500) String address,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 20) String pincode,
    @Size(max = 180) String landmark) {
}
