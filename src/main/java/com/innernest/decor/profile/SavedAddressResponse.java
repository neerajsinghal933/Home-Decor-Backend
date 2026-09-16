package com.innernest.decor.profile;

public record SavedAddressResponse(
    Long id,
    String fullName,
    String phone,
    String address,
    String city,
    String state,
    String pincode,
    String landmark) {
  static SavedAddressResponse from(SavedAddress address) {
    return new SavedAddressResponse(
        address.getId(),
        address.getFullName(),
        address.getPhone(),
        address.getAddress(),
        address.getCity(),
        address.getState(),
        address.getPincode(),
        address.getLandmark());
  }
}
