package com.example.store.service;

import com.example.store.dto.PageResponse;
import com.example.store.dto.ProductCreateDTO;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductOrderIdProjection;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductDTO> getAllProducts(int page, int size) {
        Page<Product> productPage = productRepository.findAll(PageRequest.of(page - 1, size));
        List<ProductDTO> content = productMapper.productsToProductDTOs(productPage.getContent());
        attachOrderIds(content);
        return PageResponse.of(content, productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product =
                productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
        ProductDTO productDTO = productMapper.productToProductDTO(product);
        attachOrderIds(List.of(productDTO));
        return productDTO;
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateDTO productCreateDTO) {
        Product product = productMapper.productCreateDTOToProduct(productCreateDTO);
        ProductDTO productDTO = productMapper.productToProductDTO(productRepository.save(product));
        // A brand-new product has no orders yet - no need for the batched lookup.
        productDTO.setOrders(Collections.emptyList());
        return productDTO;
    }

    // Batched regardless of how many products are being assembled: one extra
    // query for the whole page, not one per product.
    private void attachOrderIds(List<ProductDTO> products) {
        if (products.isEmpty()) {
            return;
        }

        List<Long> productIds = products.stream().map(ProductDTO::getId).toList();
        Map<Long, List<Long>> orderIdsByProductId = productRepository.findOrderIdsByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductOrderIdProjection::getProductId,
                        Collectors.mapping(ProductOrderIdProjection::getOrderId, Collectors.toList())));

        for (ProductDTO product : products) {
            product.setOrders(orderIdsByProductId.getOrDefault(product.getId(), Collections.emptyList()));
        }
    }
}
