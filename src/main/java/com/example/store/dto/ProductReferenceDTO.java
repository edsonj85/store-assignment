package com.example.store.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ProductReferenceDTO {

    @NotNull private Long id;
}
