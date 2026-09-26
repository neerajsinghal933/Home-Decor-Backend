package com.innernest.decor.order;

import java.util.List;

public record OrderLifecycleResponse(OrderResponse order, boolean canCancel, boolean canReturn,
                                     OrderRequestResponse request, List<RefundResponse> refunds,
                                     List<OrderHistoryResponse> history) {
}
