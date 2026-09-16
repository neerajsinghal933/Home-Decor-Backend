package com.innernest.decor.profile;

import com.innernest.decor.user.UserResponse;
import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.ResourceNotFoundException;
import com.innernest.decor.security.SecuritySupport;
import com.innernest.decor.storage.StorageService;
import com.innernest.decor.storage.StoredObject;
import com.innernest.decor.user.User;
import com.innernest.decor.user.UserRepository;
import java.io.IOException;
import java.util.Locale;
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

  ProfileService(UserRepository users, SavedAddressRepository addresses, StorageService storage) {
    this.users = users;
    this.addresses = addresses;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public ProfileResponse get() {
    User user = currentUser();
    return response(user);
  }

  @Transactional
  public ProfileResponse update(ProfileUpdateRequest request) {
    User user = currentUser();
    user.setName(request.name().trim());
    user.setPhone(blankToNull(request.phone()));
    user.setProfileImageUrl(blankToNull(request.profileImageUrl()));
    return response(users.save(user));
  }

  @Transactional
  public ProfileResponse updateImage(MultipartFile file) throws IOException {
    if (file.isEmpty()) throw new BusinessRuleException("Profile image is required");
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new BusinessRuleException("Uploaded file must be an image");
    }
    String extension = switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".img";
    };
    StoredObject stored = storage.store("profile-images/" + UUID.randomUUID() + extension, contentType, file.getInputStream());
    User user = currentUser();
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
