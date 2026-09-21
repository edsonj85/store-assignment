package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CustomerCreateDTO {

    @NotBlank
    private String name;
}
