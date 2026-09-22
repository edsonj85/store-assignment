package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ProductCreateDTO {

    @NotBlank
    private String description;
}
