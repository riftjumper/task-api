package com.example.task_api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class OrderRollbackIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @MockitoSpyBean
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Test
    void rollsBackOrderWhenStockSaveFails() throws Exception {
        Product product = products.save(new Product("Mouse", new BigDecimal("50.00"), 2));
        long orderCountBefore = orders.count();
        doThrow(new IllegalStateException("Simulated stock write failure"))
                .when(products).saveAndFlush(any(Product.class));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":" + product.getId() + ",\"quantity\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        assertThat(orders.count()).isEqualTo(orderCountBefore);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(2);
    }
}
