package com.innernest.decor.order;

public record OrderCustomerResponse(String fullName, String phone, String email, String address, String city, String state, String pincode, String landmark) {
}
