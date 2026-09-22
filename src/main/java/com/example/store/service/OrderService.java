package com.example.store.service;

import com.example.store.dto.OrderCreateDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.PageResponse;

public interface OrderService {

    PageResponse<OrderDTO> getAllOrders(int page, int size);

    OrderDTO getOrderById(Long id);

    OrderDTO createOrder(OrderCreateDTO orderCreateDTO);
}
