package com.microfinance.repository;

import com.microfinance.entity.CapitalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CapitalTransactionRepository extends JpaRepository<CapitalTransaction, Long> {

    List<CapitalTransaction> findAllByOrderByTransactionDateDescCreatedAtDesc();
}
