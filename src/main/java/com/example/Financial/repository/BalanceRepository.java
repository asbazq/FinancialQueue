package com.example.Financial.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Financial.model.Account;

public interface BalanceRepository extends JpaRepository<Account, Long> {
    
}
