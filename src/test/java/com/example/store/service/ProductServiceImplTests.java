package com.example.store.service;

import com.example.store.dto.PageResponse;
import com.example.store.dto.ProductCreateDTO;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductOrderIdProjection;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, productMapper);
    }

    @Test
    void getAllProductsAttachesOrderIdsGroupedByProduct() {
        Product widget = new Product();
        widget.setId(1L);
        widget.setDescription("Widget");

        Product gadget = new Product();
        gadget.setId(2L);
        gadget.setDescription("Gadget");

        ProductDTO widgetDTO = new ProductDTO();
        widgetDTO.setId(1L);
        widgetDTO.setDescription("Widget");

        ProductDTO gadgetDTO = new ProductDTO();
        gadgetDTO.setId(2L);
        gadgetDTO.setDescription("Gadget");

        // page=2 -> Pageable offset 1
        Pageable pageable = PageRequest.of(1, 20);
        PageImpl<Product> productPage = new PageImpl<>(List.of(widget, gadget), pageable, 22);

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productMapper.productsToProductDTOs(List.of(widget, gadget))).thenReturn(List.of(widgetDTO, gadgetDTO));
        when(productRepository.findOrderIdsByProductIds(List.of(1L, 2L)))
                .thenReturn(List.of(projection(1L, 10L), projection(1L, 11L)));

        PageResponse<ProductDTO> result = productService.getAllProducts(2, 20);

        assertThat(result.getContent()).containsExactly(widgetDTO, gadgetDTO);
        assertThat(widgetDTO.getOrders()).containsExactly(10L, 11L);
        assertThat(gadgetDTO.getOrders()).isEmpty();
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(22);
    }

    @Test
    void getAllProductsSkipsOrderIdLookupWhenPageIsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productRepository.findAll(pageable)).thenReturn(emptyPage);
        when(productMapper.productsToProductDTOs(List.of())).thenReturn(List.of());

        PageResponse<ProductDTO> result = productService.getAllProducts(1, 20);

        assertThat(result.getContent()).isEmpty();
        verifyNoOrderIdLookup();
    }

    @Test
    void getProductByIdReturnsDTOWithOrderIds() {
        Product product = new Product();
        product.setId(1L);
        product.setDescription("Widget");

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setDescription("Widget");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.productToProductDTO(product)).thenReturn(productDTO);
        when(productRepository.findOrderIdsByProductIds(List.of(1L))).thenReturn(List.of(projection(1L, 10L)));

        ProductDTO result = productService.getProductById(1L);

        assertThat(result.getOrders()).containsExactly(10L);
    }

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProductSavesAndReturnsDTOWithNoOrders() {
        ProductCreateDTO request = new ProductCreateDTO();
        request.setDescription("Widget");

        Product mapped = new Product();
        mapped.setDescription("Widget");

        Product saved = new Product();
        saved.setId(1L);
        saved.setDescription("Widget");

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setDescription("Widget");

        when(productMapper.productCreateDTOToProduct(request)).thenReturn(mapped);
        when(productRepository.save(mapped)).thenReturn(saved);
        when(productMapper.productToProductDTO(saved)).thenReturn(productDTO);

        ProductDTO result = productService.createProduct(request);

        assertThat(result.getOrders()).isEmpty();
        verifyNoOrderIdLookup();
    }

    private void verifyNoOrderIdLookup() {
        verify(productRepository, never()).findOrderIdsByProductIds(anyList());
    }

    private static ProductOrderIdProjection projection(Long productId, Long orderId) {
        return new ProductOrderIdProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getOrderId() {
                return orderId;
            }
        };
    }
}
