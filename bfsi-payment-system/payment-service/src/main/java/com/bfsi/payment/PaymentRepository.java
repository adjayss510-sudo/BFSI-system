package com.bfsi.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserIdOrderByIdDesc(Long userId);
    List<Payment> findAllByOrderByIdDesc();
    Optional<Payment> findByTxnId(String txnId);
    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, String key);
}
