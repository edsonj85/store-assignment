package com.example.store.mapper;

import com.example.store.dto.ProductCreateDTO;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "orders", ignore = true)
    ProductDTO productToProductDTO(Product product);

    List<ProductDTO> productsToProductDTOs(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orders", ignore = true)
    Product productCreateDTOToProduct(ProductCreateDTO productCreateDTO);
}
