package com.example.store.service;

import com.example.store.dto.PageResponse;
import com.example.store.dto.ProductCreateDTO;
import com.example.store.dto.ProductDTO;

public interface ProductService {

    PageResponse<ProductDTO> getAllProducts(int page, int size);

    ProductDTO getProductById(Long id);

    ProductDTO createProduct(ProductCreateDTO productCreateDTO);
}
