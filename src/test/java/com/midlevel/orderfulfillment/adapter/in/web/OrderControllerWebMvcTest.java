package com.midlevel.orderfulfillment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midlevel.orderfulfillment.adapter.in.web.dto.AddressDto;
import com.midlevel.orderfulfillment.adapter.in.web.dto.CreateOrderRequest;
import com.midlevel.orderfulfillment.adapter.in.web.dto.MoneyDto;
import com.midlevel.orderfulfillment.adapter.in.web.mapper.OrderDtoMapper;
import com.midlevel.orderfulfillment.application.OrderService;
import com.midlevel.orderfulfillment.adapter.in.web.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@Import(OrderDtoMapper.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController API tests (SpringBootTest + MockMvc)")
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;
    
    @MockBean
    private com.midlevel.orderfulfillment.config.security.JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = {"CUSTOMER"})
    @DisplayName("POST /api/orders creates order and returns 201")
    void createOrder() throws Exception {
    CreateOrderRequest request = new CreateOrderRequest(
        "CUST-1",
        new AddressDto("123 Main", "SF", "CA", "94105", "US"),
        List.of(
            new CreateOrderRequest.OrderItemRequest("P1", "Widget", new MoneyDto(BigDecimal.valueOf(10.00), "USD"), 2),
            new CreateOrderRequest.OrderItemRequest("P2", "Gadget", new MoneyDto(BigDecimal.valueOf(5.00), "USD"), 1)
        )
    );

    // Stub service to return a domain order
    com.midlevel.orderfulfillment.domain.model.Address address = new com.midlevel.orderfulfillment.domain.model.Address("123 Main", "SF", "CA", "94105", "US");
    java.util.List<com.midlevel.orderfulfillment.domain.model.OrderItem> items = java.util.List.of(
        com.midlevel.orderfulfillment.domain.model.OrderItem.create(
            "P1", "Widget", com.midlevel.orderfulfillment.domain.model.Money.usd(BigDecimal.valueOf(10.00)), 2
        ),
        com.midlevel.orderfulfillment.domain.model.OrderItem.create(
            "P2", "Gadget", com.midlevel.orderfulfillment.domain.model.Money.usd(BigDecimal.valueOf(5.00)), 1
        )
    );
    com.midlevel.orderfulfillment.domain.model.Order order = com.midlevel.orderfulfillment.domain.model.Order.create("CUST-1", items, address);

    org.mockito.Mockito.when(orderService.createOrder(org.mockito.ArgumentMatchers.eq("CUST-1"), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(com.midlevel.orderfulfillment.domain.model.Address.class)))
        .thenReturn(order);

    mvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.orderId").exists())
        .andExpect(jsonPath("$.customerId").value("CUST-1"))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.items[0].productId").value("P1"))
        .andExpect(jsonPath("$.items[1].productId").value("P2"));
    }
}
