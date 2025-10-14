package com.paymentchain.customer.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerProductCreateRequest {

    @NotNull(message = "productId es obligatorio")
    private Long productId;
}