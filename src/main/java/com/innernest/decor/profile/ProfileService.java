package com.innernest.decor.profile;

import com.innernest.decor.user.UserResponse;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StorageCleanup;
import com.innernest.decor.storage.ImageUploadValidator;
import com.innernest.decor.storage.StoredObject;
import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRepository;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
  private final UserRepository users;
  private final SavedAddressRepository addresses;
  private final StorageService storage;
  private final StorageCleanup storageCleanup;

  ProfileService(UserRepository users, SavedAddressRepository addresses, StorageService storage, StorageCleanup storageCleanup) {
    this.users = users;
    this.addresses = addresses;
    this.storage = storage;
    this.storageCleanup = storageCleanup;
  }

  @Transactional(readOnly = true)
  public ProfileResponse get() {
    User user = currentUser();
    return response(user);
  }

  @Transactional
  public ProfileResponse update(ProfileUpdateRequest request) {
    User user = currentUser();
    String oldProfileImageUrl = user.getProfileImageUrl();
    user.setName(request.name().trim());
    user.setPhone(blankToNull(request.phone()));
    user.setProfileImageUrl(blankToNull(request.profileImageUrl()));
    User saved = users.save(user);
    if (!java.util.Objects.equals(oldProfileImageUrl, saved.getProfileImageUrl())) {
      storageCleanup.deleteAfterCommit(oldProfileImageUrl, "profile-images/");
    }
    return response(saved);
  }

  @Transactional
  public ProfileResponse updateImage(MultipartFile file) throws IOException {
    ImageUploadValidator.ValidatedImage image = ImageUploadValidator.validate(file, "Profile image is required");
    User user = currentUser();
    String key = "profile-images/" + UUID.randomUUID() + "." + image.extension();
    StoredObject stored;
    try (var input = file.getInputStream()) {
      stored = storage.store(key, image.contentType(), file.getSize(), input);
    }
    storageCleanup.replaceAfterTransaction(user.getProfileImageUrl(), key, "profile-images/");
    user.setProfileImageUrl(stored.url());
    return response(users.save(user));
  }

  @Transactional
  public ProfileResponse addAddress(SavedAddressRequest request) {
    User user = currentUser();
    SavedAddress address = new SavedAddress();
    apply(address, request);
    address.setUser(user);
    addresses.save(address);
    return response(user);
  }

  @Transactional
  public ProfileResponse updateAddress(Long id, SavedAddressRequest request) {
    User user = currentUser();
    SavedAddress address = addresses.findById(id)
        .filter(row -> row.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    apply(address, request);
    return response(user);
  }

  @Transactional
  public ProfileResponse deleteAddress(Long id) {
    User user = currentUser();
    SavedAddress address = addresses.findById(id)
        .filter(row -> row.getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    addresses.delete(address);
    return response(user);
  }

  private ProfileResponse response(User user) {
    return new ProfileResponse(
        UserResponse.from(user),
        addresses.findByUserIdOrderByIdDesc(user.getId()).stream().map(SavedAddressResponse::from).toList());
  }

  private User currentUser() {
    Long id = SecuritySupport.currentUser().orElseThrow(() -> new AccessDeniedException("Unauthorized")).id();
    return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private void apply(SavedAddress address, SavedAddressRequest request) {
    address.setFullName(request.fullName().trim());
    address.setPhone(blankToNull(request.phone()));
    address.setAddress(request.address().trim());
    address.setCity(blankToNull(request.city()));
    address.setState(blankToNull(request.state()));
    address.setPincode(blankToNull(request.pincode()));
    address.setLandmark(blankToNull(request.landmark()));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
