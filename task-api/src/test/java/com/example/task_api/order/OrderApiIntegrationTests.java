package com.example.task_api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class OrderApiIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @BeforeEach
    void clearOrdersAndProducts() {
        orders.deleteAll();
        products.deleteAll();
    }

    @Test
    void createsOrderUsingDatabasePriceAndReducesStock() throws Exception {
        Product product = products.save(new Product("Keyboard", new BigDecimal("12500.00"), 5));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":" + product.getId()
                                + ",\"quantity\":2,\"total_price\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product_id").value(product.getId()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.total_price").value(25000.00))
                .andExpect(jsonPath("$.status").value("CREATED"));

        assertThat(orders.count()).isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void rejectsMissingZeroNegativeAndNonIntegerQuantity() throws Exception {
        Product product = products.save(new Product("Mouse", new BigDecimal("50.00"), 5));

        for (String quantity : new String[] {"null", "0", "-1", "1.5"}) {
            mvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"product_id\":" + product.getId()
                                    + ",\"quantity\":" + quantity + "}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        assertThat(orders.count()).isZero();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void rejectsMissingProductId() throws Exception {
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields.product_id").exists());
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":9999,\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        assertThat(orders.count()).isZero();
    }

    @Test
    void rejectsInsufficientStockWithoutCreatingOrder() throws Exception {
        Product product = products.save(new Product("Mouse", new BigDecimal("50.00"), 1));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":" + product.getId() + ",\"quantity\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertThat(orders.count()).isZero();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void allowsQuantityExactlyEqualToStockAndGetsSavedOrder() throws Exception {
        Product product = products.save(new Product("Mouse", new BigDecimal("50.00"), 2));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":" + product.getId() + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        Long orderId = orders.findAll().get(0).getId();
        mvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.total_price").value(100.00));
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mvc.perform(get("/orders/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void rejectsNonNumericOrderId() throws Exception {
        mvc.perform(get("/orders/bukan-angka"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PATH"));
    }
}
