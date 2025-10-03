package com.paymentchain.transaction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentchain.transaction.entities.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
    
    Optional<Transaction> findByIbanAccount(String code);
}
