package com.paymentchain.customer.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerCreateRequest {

    @NotBlank(message = "name es obligatorio")
    private String name;

    @NotBlank(message = "phone es obligatorio")
    private String phone;

    @NotBlank(message = "code es obligatorio")
    private String code;

    @NotBlank(message = "iban es obligatorio")
    private String iban;

    @NotBlank(message = "surname es obligatorio")
    private String surname;

    @NotBlank(message = "address es obligatorio")
    private String address;

    @Valid
    @NotNull(message = "products no puede ser null")
    @Size(min = 1, message = "debe registrar al menos 1 product")
    private List<CustomerProductCreateRequest> products;
}