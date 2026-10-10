package com.example.task_api.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        int quantity = request.quantity().intValueExact();
        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new OrderApiException(
                        OrderApiException.Reason.PRODUCT_NOT_FOUND,
                        "Produk tidak ditemukan"));

        if (product.getStock() < quantity) {
            throw new OrderApiException(
                    OrderApiException.Reason.INSUFFICIENT_STOCK,
                    "Stok produk tidak cukup");
        }

        Order order = orders.saveAndFlush(new Order(
                product,
                quantity,
                product.getPrice().multiply(BigDecimal.valueOf(quantity))));
        product.decreaseStock(quantity);
        products.saveAndFlush(product);

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new OrderApiException(
                        OrderApiException.Reason.ORDER_NOT_FOUND,
                        "Order tidak ditemukan"));
        return OrderResponse.from(order);
    }
}
