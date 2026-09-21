package com.microfinance.repository;

import com.microfinance.entity.CapitalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapitalAccountRepository extends JpaRepository<CapitalAccount, Long> {
}
