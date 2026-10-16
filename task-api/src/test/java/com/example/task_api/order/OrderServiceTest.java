package com.example.task_api.order;

import org.assertj.core.api.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static com.example.task_api.order.OrderApiException.Reason.INSUFFICIENT_STOCK;
import static com.example.task_api.order.OrderApiException.Reason.PRODUCT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private OrderRepository orders;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_berhasilMembuatOrderDanMengurangiStock() {
        Product product = new Product("Keyboard", new BigDecimal("10000.00"), 5);
        ReflectionTestUtils.setField(product, "id", 1L);

        // Product ditemukan, jadi service boleh lanjut membuat order.
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        // Di aplikasi asli ID order dibuat database. Di unit test, ID kita isi manual.
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 10L);
            return order;
        });

        // Simpan product yang stock-nya sudah dikurangi, lalu kembalikan object yang sama.
        when(products.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(1L, new BigDecimal("2"));

        OrderResponse response = orderService.create(request);

        assertThat(response).isEqualTo(new OrderResponse(
                10L,
                1L,
                2,
                new BigDecimal("20000.00"),
                "CREATED"));
        assertThat(product.getStock()).isEqualTo(3);
        verify(orders).saveAndFlush(any(Order.class));
        verify(products).saveAndFlush(product);
    }

    @Nested
    class NegativeFlow {
        @Test
//        @Disabled("TODO: latihan negative flow")
        void create_gagalJikaProductTidakDitemukan() {
            // Setup:
            // products.findByIdForUpdate(productId) mengembalikan Optional.empty().
            //
            // Expected:
            // - service melempar OrderApiException
            // - reason = PRODUCT_NOT_FOUND
            // - orders.saveAndFlush tidak pernah dipanggil
            // - products.saveAndFlush tidak pernah dipanggil

            when(products.findByIdForUpdate(1L))
                    .thenReturn(Optional.empty());

            CreateOrderRequest request =
                    new CreateOrderRequest(
                            1L,
                            new BigDecimal("2")
                    );

            Throwable thrown = catchThrowable(
                    () -> orderService.create(request)
            );

            assertThat(thrown)
                    .isInstanceOf(OrderApiException.class);

            assertThat(((OrderApiException) thrown).getReason())
                    .isEqualTo(PRODUCT_NOT_FOUND);

            verify(orders, never())
                    .saveAndFlush(any(Order.class));

            verify(products, never())
                    .saveAndFlush(any(Product.class));

        }

        @Test
//        @Disabled("TODO: latihan negative flow")
        void create_gagalJikaStockTidakCukup() {
            // Setup:
            // product tersedia, misalnya stock = 1.
            // request quantity = 2.
            //
            // Expected:
            // - service melempar OrderApiException
            // - reason = INSUFFICIENT_STOCK
            // - order tidak dibuat
            // - stock product tetap 1
            // - orders.saveAndFlush tidak pernah dipanggil
            // - products.saveAndFlush tidak pernah dipanggil

            Product product = new Product(
                    "Keyboard",
                    new BigDecimal("10000.00"),
                    1
            );

            ReflectionTestUtils.setField(
                    product,
                    "id",
                    1L
            );

            when(products.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(product));

            CreateOrderRequest request =
                    new CreateOrderRequest(
                            1L,
                            new BigDecimal("2")
                    );

            Throwable thrown = catchThrowable(
                    ()->orderService.create(request)
            );

            assertThat(thrown)
                    .isInstanceOf(OrderApiException.class);

            assertThat(((OrderApiException) thrown).getReason())
                    .isEqualTo(INSUFFICIENT_STOCK);

            assertThat(product.getStock())
                    .isEqualTo(1);

            verify(orders, never())
                    .saveAndFlush(any(Order.class));

            verify(products, never())
                    .saveAndFlush(any(Product.class));
        }
    }

    @Nested
    class EdgeCaseDanValidasi {
        @Test
//        @Disabled("TODO: latihan edge case")
        void create_berhasilJikaQuantitySamaDenganStock() {
            // Setup:
            // product tersedia dengan stock = 2.
            // request quantity = 2.
            //
            // Expected:
            // - order berhasil dibuat
            // - totalPrice = product.price * quantity
            // - stock product menjadi 0
            // - response status = CREATED

            Product product = new Product(
                    "mouse",
                    new BigDecimal("50000.00"),
                    2
            );

            ReflectionTestUtils.setField(
                    product,
                    "id",
                    1L
            );

            when(products.findByIdForUpdate(1L))
                    .thenReturn(Optional.of(product));

//            Mock the order save
            when(orders.saveAndFlush(any(Order.class)))
                    .thenAnswer(invocation -> {
                       Order order = invocation.getArgument(0);
                       ReflectionTestUtils.setField(order, "id", 10L);
                        return order;
                    });

//            mock product save
            when(products.saveAndFlush(any(Product.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CreateOrderRequest request = new CreateOrderRequest(
              1L,
              new BigDecimal("2")
            );

            OrderResponse response = orderService.create(request);

            assertThat(response).isEqualTo(new OrderResponse(
               10L,
               1L,
               2,
               new BigDecimal("100000.00"),
               "CREATED"
            ));

            assertThat(product.getStock()).isEqualTo(0);

            verify(orders).saveAndFlush(any(Order.class));
            verify(products).saveAndFlush(product);

        }

        @Test
//        @Disabled("TODO: latihan validasi input")
        void create_gagalJikaQuantityBukanInteger() {
            // Catatan:
            // Saat ini validasi non-integer terutama terjadi di layer controller
            // lewat @Digits pada CreateOrderRequest.
            //
            // Expected di service jika tetap dipanggil langsung:
            // - request quantity 1.5 akan gagal saat intValueExact()
            // - exception yang muncul adalah ArithmeticException
            //
            // Expected di controller/integration test:
            // - HTTP 422
            // - response code = INVALID_INPUT

//            Product product = new Product(
//                    "Mouse",
//                    new BigDecimal("50000.00"),
//                    5
//            );
//
//            ReflectionTestUtils.setField(product, "id", 1L);
//
//            when(products.findByIdForUpdate(1L))
//                    .thenReturn(Optional.of(product));

            CreateOrderRequest request = new CreateOrderRequest(
                    1L,
                    new BigDecimal("1.5")
            );

            Throwable thrown = catchThrowable(
                    () -> orderService.create(request)
            );

            assertThat(thrown)
                    .isInstanceOf(ArithmeticException.class);

            verify(orders, never()).saveAndFlush(any(Order.class));

            verify(products, never()).saveAndFlush(any(Product.class));
        }
    }

    @Nested
    class FindById {
        @Test
//        @Disabled("TODO: latihan happy flow")
        void findById_berhasilJikaOrderAda() {
            // Setup:
            // orders.findById(id) mengembalikan order.
            //
            // Expected:
            // - response berisi id order, productId, quantity, totalPrice, dan status CREATED

//            Order order = new Order(
//                    1L,
//                    2,
//                    new BigDecimal("100000.00"),
//                    "CREATED"
//            );

            ReflectionTestUtils.setField(order, "id", 10L);
        }

        @Test
//        @Disabled("TODO: latihan negative flow")
        void findById_gagalJikaOrderTidakAda() {
            // Setup:
            // orders.findById(id) mengembalikan Optional.empty().
            //
            // Expected:
            // - service melempar OrderApiException
            // - reason = ORDER_NOT_FOUND
        }
    }
}
