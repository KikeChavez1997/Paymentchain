package com.paymentchain.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentchain.billing.entities.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
}
