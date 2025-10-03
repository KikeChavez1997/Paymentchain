package com.paymentchain.transaction.enums;

public enum TransactionStatus {
    PENDING("01"),
    SETTLED("02"),
    REJECTED("03"),
    CANCELED("04");

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    // 🔄 Conversión de código a enum
    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Código inválido: " + code);
    }
}
