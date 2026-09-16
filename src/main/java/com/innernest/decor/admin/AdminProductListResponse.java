package com.innernest.decor.admin;

import com.innernest.decor.catalog.ProductResponse;
import java.util.List;
public record AdminProductListResponse(List<ProductResponse> items, long total, int page, int size, int totalPages) {}
