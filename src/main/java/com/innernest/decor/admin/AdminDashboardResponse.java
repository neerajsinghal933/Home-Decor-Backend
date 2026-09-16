package com.innernest.decor.admin;

import com.innernest.decor.order.OrderResponse;
import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
    long totalProducts,
    long activeProducts,
    long lowStockProducts,
    long outOfStockProducts,
    long totalOrders,
    Map<String, Long> ordersByStatus,
    long totalCustomers,
    List<OrderResponse> recentOrders) {
}

