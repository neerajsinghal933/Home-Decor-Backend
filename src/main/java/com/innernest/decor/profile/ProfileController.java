package com.innernest.decor.profile;

import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final ProfileService service;

  ProfileController(ProfileService service) {
    this.service = service;
  }

  @GetMapping
  ProfileResponse get() {
    return service.get();
  }

  @PutMapping
  ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
    return service.update(request);
  }

  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ProfileResponse updateImage(@RequestPart("file") MultipartFile file) throws IOException {
    return service.updateImage(file);
  }

  @PostMapping("/addresses")
  ProfileResponse addAddress(@Valid @RequestBody SavedAddressRequest request) {
    return service.addAddress(request);
  }

  @PutMapping("/addresses/{id}")
  ProfileResponse updateAddress(@PathVariable Long id, @Valid @RequestBody SavedAddressRequest request) {
    return service.updateAddress(id, request);
  }

  @DeleteMapping("/addresses/{id}")
  ProfileResponse deleteAddress(@PathVariable Long id) {
    return service.deleteAddress(id);
  }
}
