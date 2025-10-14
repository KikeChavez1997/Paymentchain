package com.paymentchain.transaction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentchain.transaction.entities.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
    
    List<Transaction> findByAccountIban(String accountIban);
}
