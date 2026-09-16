package com.innernest.decor.promo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromoCodeController {
  private final PromoCodeService service;

  PromoCodeController(PromoCodeService service) {
    this.service = service;
  }

  @PostMapping("/api/promos/validate")
  PromoValidationResponse validate(@Valid @RequestBody PromoValidationRequest request) {
    return service.validate(request);
  }

  @GetMapping("/api/admin/promos")
  List<PromoCodeResponse> all() {
    return service.all();
  }

  @PostMapping("/api/admin/promos")
  PromoCodeResponse create(@Valid @RequestBody PromoCodeRequest request) {
    return service.create(request);
  }

  @PutMapping("/api/admin/promos/{id}")
  PromoCodeResponse update(@PathVariable Long id, @Valid @RequestBody PromoCodeRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/api/admin/promos/{id}")
  PromoCodeResponse delete(@PathVariable Long id) {
    return service.delete(id);
  }
}
