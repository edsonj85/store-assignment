package com.example.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotBlank
    private String description;

    @NotNull
    @Valid
    private CustomerReferenceDTO customer;
}
