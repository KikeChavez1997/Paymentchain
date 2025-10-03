package com.paymentchain.product.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // (opcional) más común con autoincrement
    private Long id;

    // (opcional pero recomendado) habilita locking optimista “real”
    // @Version
    // private Long version;

    private String code;
    private String name;
}
