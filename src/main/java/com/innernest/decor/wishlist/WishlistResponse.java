package com.innernest.decor.wishlist;

import com.innernest.decor.catalog.ProductResponse;
import java.util.List;

public record WishlistResponse(List<ProductResponse> items) {
}

