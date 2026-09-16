package com.innernest.decor.promo;

import com.innernest.decor.common.BusinessRuleException;
import com.innernest.decor.common.DuplicateResourceException;
import com.innernest.decor.common.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoCodeService {
  private final PromoCodeRepository promos;

  PromoCodeService(PromoCodeRepository promos) {
    this.promos = promos;
  }

  @Transactional(readOnly = true)
  public List<PromoCodeResponse> all() {
    return promos.findAll().stream().map(PromoCodeResponse::from).toList();
  }

  @Transactional
  public PromoCodeResponse create(PromoCodeRequest request) {
    PromoCode promo = new PromoCode();
    rejectDuplicateCode(request.code(), null);
    apply(promo, request);
    return PromoCodeResponse.from(promos.save(promo));
  }

  @Transactional
  public PromoCodeResponse update(Long id, PromoCodeRequest request) {
    PromoCode promo = promos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
    rejectDuplicateCode(request.code(), id);
    apply(promo, request);
    return PromoCodeResponse.from(promos.save(promo));
  }

  @Transactional
  public PromoCodeResponse delete(Long id) {
    PromoCode promo = promos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
    promo.setActive(false);
    return PromoCodeResponse.from(promos.save(promo));
  }

  @Transactional(readOnly = true)
  public PromoValidationResponse validate(PromoValidationRequest request) {
    BigDecimal discount = discountFor(request.code(), request.subtotal());
    return new PromoValidationResponse(request.code().trim().toUpperCase(), discount, "Promo applied");
  }

  @Transactional(readOnly = true)
  public BigDecimal discountFor(String code, BigDecimal subtotal) {
    if (code == null || code.isBlank()) return BigDecimal.ZERO.setScale(2);
    PromoCode promo = promos.findByCodeIgnoreCase(code.trim())
        .filter(PromoCode::isActive)
        .orElseThrow(() -> new BusinessRuleException("Invalid promo code"));
    if (subtotal.compareTo(promo.getMinimumOrderAmount()) < 0) {
      throw new BusinessRuleException("Promo requires minimum order of ₹" + promo.getMinimumOrderAmount().setScale(0, RoundingMode.HALF_UP));
    }
    BigDecimal discount = promo.getDiscountType() == DiscountType.PERCENT
        ? subtotal.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        : promo.getDiscountValue();
    return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
  }

  private void apply(PromoCode promo, PromoCodeRequest request) {
    promo.setCode(request.code().trim().toUpperCase());
    promo.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
    promo.setDiscountType(request.discountType());
    promo.setDiscountValue(request.discountValue());
    promo.setMinimumOrderAmount(request.minimumOrderAmount());
    promo.setActive(request.active() == null || request.active());
  }

  private void rejectDuplicateCode(String code, Long currentId) {
    promos.findByCodeIgnoreCase(code.trim()).filter(existing -> !existing.getId().equals(currentId)).ifPresent(existing -> {
      throw new DuplicateResourceException("Promo code already exists");
    });
  }
}
