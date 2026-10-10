package com.example.task_api.order;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    void create() {

        //mockito junit

//        Mockito.when(orderService.create()).then()

        CreateOrderRequest createOrderRequest = new CreateOrderRequest(1L, BigDecimal.ONE);
        OrderResponse expectedOrderResponse = new OrderResponse(1L, 1L, 1,
                BigDecimal.TEN, "COMPLETE");

        OrderResponse createOrder = orderService.create(createOrderRequest);
        assertEquals(expectedOrderResponse, createOrder);

        // bikin 1 happy path/berhasil create order

        // bikin 1 gagal kena validasi OrderApiException (stock kurang, product tidak ada dll)
    }

    @Test
    void findById() {
    }
}