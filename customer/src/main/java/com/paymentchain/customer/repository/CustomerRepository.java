package com.paymentchain.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentchain.customer.entities.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCode(String code);

     // Método que SÍ trae products
    @EntityGraph(attributePaths = "products")
    Optional<Customer> findOneByCode(String code);

    Optional<Customer> findByIban(String iban);

}
