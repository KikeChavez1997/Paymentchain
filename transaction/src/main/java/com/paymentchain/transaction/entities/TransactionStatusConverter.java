package com.paymentchain.transaction.entities;

import com.paymentchain.transaction.enums.TransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransactionStatus status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String code) {
        return code != null ? TransactionStatus.fromCode(code) : null;
    }
}
