package com.bfsi.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByUserIdOrderByIdDesc(Long userId);
    List<LedgerEntry> findAllByOrderByIdDesc();
    List<LedgerEntry> findByTxnIdOrderByIdAsc(String txnId);
}

interface AuditRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByIdDesc();
}
