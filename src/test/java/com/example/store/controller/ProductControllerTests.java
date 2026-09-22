package com.example.store.controller;

import com.example.store.dto.PageResponse;
import com.example.store.dto.ProductCreateDTO;
import com.example.store.dto.ProductDTO;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductCreateDTO createRequest;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        createRequest = new ProductCreateDTO();
        createRequest.setDescription("Widget");

        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setDescription("Widget");
        productDTO.setOrders(List.of());
    }

    @Test
    void testCreateProduct() throws Exception {
        when(productService.createProduct(createRequest)).thenReturn(productDTO);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    void testCreateProductBlankDescriptionReturns400() throws Exception {
        createRequest.setDescription(" ");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProductAllowsDuplicateDescription() throws Exception {
        // Products have no business key beyond id (4.1/4.2) - a repeated
        // description is not a conflict.
        ProductDTO secondProductDTO = new ProductDTO();
        secondProductDTO.setId(2L);
        secondProductDTO.setDescription("Widget");
        secondProductDTO.setOrders(List.of());

        when(productService.createProduct(createRequest)).thenReturn(productDTO).thenReturn(secondProductDTO);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void testGetProductByIdFound() throws Exception {
        productDTO.setOrders(List.of(5L, 6L));
        when(productService.getProductById(1L)).thenReturn(productDTO);

        mockMvc.perform(get("/products/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orders", Matchers.containsInAnyOrder(5, 6)));
    }

    @Test
    void testGetProductByIdNotFound() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/products/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Product 999 not found"));
    }

    @Test
    void testGetProductByIdNonNumericReturns400() throws Exception {
        mockMvc.perform(get("/products/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetProductsUsesDefaultPageAndSize() throws Exception {
        // page=1 -> Pageable offset 0
        PageResponse<ProductDTO> page = PageResponse.of(List.of(productDTO), zeroIndexedPageOf(0, 20, 1));
        when(productService.getAllProducts(1, 20)).thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Widget"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetProductsSizeOverMaxReturns400() throws Exception {
        mockMvc.perform(get("/products").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetProductsPageZeroReturns400() throws Exception {
        mockMvc.perform(get("/products").param("page", "0")).andExpect(status().isBadRequest());
    }

    private static Page<Object> zeroIndexedPageOf(int zeroIndexedPage, int size, long totalElements) {
        return new PageImpl<>(List.of(), PageRequest.of(zeroIndexedPage, size), totalElements);
    }
}
