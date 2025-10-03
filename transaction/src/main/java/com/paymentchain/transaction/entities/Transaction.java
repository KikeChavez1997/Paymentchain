package com.paymentchain.transaction.entities;

import java.time.LocalDate;

import com.paymentchain.transaction.enums.Channel;
import com.paymentchain.transaction.enums.TransactionStatus;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // (opcional) más común con autoincrement
    private Long id;
    private String reference;
    private String accountIban;
    private LocalDate date;
    private Double amount;
    private Double fee;
    private String description;

    @Convert(converter = TransactionStatusConverter.class)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING) // 👈 se guarda como "WEB", "CAJERO" o "OFICINA"
    private Channel channel;
}
