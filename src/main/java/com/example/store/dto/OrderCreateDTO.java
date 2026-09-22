package com.example.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {

    @NotBlank
    private String description;

    @NotNull @Valid
    private CustomerReferenceDTO customer;

    // Emptiness/absence is checked in the service (A3), not here with
    // @NotEmpty - "at least one product" is a business rule, not a shape check.
    @Valid
    private List<ProductReferenceDTO> products;
}
