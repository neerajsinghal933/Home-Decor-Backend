package com.innernest.decor.profile;

import com.innernest.decor.user.UserResponse;
import java.util.List;

public record ProfileResponse(UserResponse user, List<SavedAddressResponse> addresses) {
}
