UPDATE orders
SET status = 'PAYMENT_PENDING'
WHERE payment_provider = 'RAZORPAY'
  AND payment_status = 'PENDING'
  AND status = 'PLACED';
