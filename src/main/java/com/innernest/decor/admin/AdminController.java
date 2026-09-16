package com.innernest.decor.admin;

import com.innernest.decor.catalog.ProductResponse;
import com.innernest.decor.catalog.TagRequest;
import com.innernest.decor.catalog.TagResponse;
import com.innernest.decor.order.OrderResponse;
import com.innernest.decor.user.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminService service;

  AdminController(AdminService service) {
    this.service = service;
  }

  @GetMapping("/dashboard")
  AdminDashboardResponse dashboard() {
    return service.dashboard();
  }

  @GetMapping("/products")
  AdminProductListResponse products(@RequestParam(required = false) String search, @RequestParam(required = false) String category, @RequestParam(required = false) Long tagId, @RequestParam(required = false) java.math.BigDecimal minPrice, @RequestParam(required = false) java.math.BigDecimal maxPrice, @RequestParam(required = false) String stock, @RequestParam(required = false) String status, @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
    return service.products(search, category, tagId, minPrice, maxPrice, stock, status, sort, page, size);
  }

  @GetMapping("/tags") org.springframework.data.domain.Page<TagResponse> tags(@RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) { return service.tags(search, page, size); }
  @PostMapping("/tags") TagResponse createTag(@Valid @RequestBody TagRequest request) { return service.createTag(request); }
  @PutMapping("/tags/{id}") TagResponse updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) { return service.updateTag(id, request); }
  @DeleteMapping("/tags/{id}") void deleteTag(@PathVariable Long id) { service.deleteTag(id); }

  @GetMapping("/customers")
  List<UserResponse> customers() {
    return service.customers();
  }

  @PostMapping("/products")
  ProductResponse createProduct(@Valid @RequestBody AdminProductRequest request) {
    return service.createProduct(request);
  }

  @PutMapping("/products/{id}")
  ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody AdminProductRequest request) {
    return service.updateProduct(id, request);
  }

  @DeleteMapping("/products/{id}")
  ProductResponse deleteProduct(@PathVariable Long id) {
    return service.deactivateProduct(id);
  }

  @PutMapping("/products/order")
  List<ProductResponse> updateProductOrder(@Valid @RequestBody List<@Valid ProductOrderRequest> request) {
    return service.updateProductOrder(request);
  }

  @GetMapping("/orders")
  List<OrderResponse> orders(@RequestParam(required = false) String status) {
    return service.orders(status);
  }

  @GetMapping("/orders/{orderNumber}")
  OrderResponse order(@PathVariable String orderNumber) {
    return service.order(orderNumber);
  }

  @PutMapping("/orders/{orderNumber}/status")
  OrderResponse updateOrderStatus(@PathVariable String orderNumber, @Valid @RequestBody AdminOrderStatusRequest request) {
    return service.updateOrderStatus(orderNumber, request);
  }
}
