package com.midlevel.orderfulfillment.adapter.in.web.mapper;

import com.midlevel.orderfulfillment.adapter.in.web.dto.AddressDto;
import com.midlevel.orderfulfillment.adapter.in.web.dto.CreateOrderRequest;
import com.midlevel.orderfulfillment.adapter.in.web.dto.MoneyDto;
import com.midlevel.orderfulfillment.adapter.in.web.dto.OrderResponse;
import com.midlevel.orderfulfillment.domain.model.Address;
import com.midlevel.orderfulfillment.domain.model.Money;
import com.midlevel.orderfulfillment.domain.model.Order;
import com.midlevel.orderfulfillment.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Maps between DTOs (Data Transfer Objects) and Domain models.
 * 
 * Why a separate mapper class?
 * 1. Separation of Concerns: Domain doesn't know about DTOs, DTOs don't contain business logic
 * 2. Testability: Can test mapping logic independently
 * 3. Flexibility: Easy to add different mappings for different API versions
 * 4. Single Responsibility: Each class focuses on one thing
 * 
 * Alternative: Could use MapStruct for automatic mapping generation
 */
@Component
public class OrderDtoMapper {
    
    /**
     * Convert CreateOrderRequest DTO to domain Order.
     */
    public Order toDomain(CreateOrderRequest request) {
        // Convert DTO items to domain OrderItems
        var items = request.items().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
        
        // Convert DTO address to domain Address
        Address address = toAddress(request.shippingAddress());
        
        // Use domain factory method to create Order (enforces validation)
        return Order.create(
                request.customerId(),
                items,
                address
        );
    }
    
    /**
     * Convert domain Order to OrderResponse DTO.
     */
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                toAddressDto(order.getShippingAddress()),
                order.getItems().stream()
                        .map(this::toOrderItemResponse)
                        .collect(Collectors.toList()),
                toMoneyDto(order.calculateTotal())
        );
    }
    
    /**
     * Convert OrderItemRequest DTO to domain OrderItem.
     */
    private OrderItem toOrderItem(CreateOrderRequest.OrderItemRequest dto) {
        Money unitPrice = toMoney(dto.unitPrice());
        return OrderItem.create(
                dto.productId(),
                dto.productName(),
                unitPrice,
                dto.quantity()
        );
    }
    
    /**
     * Convert domain OrderItem to OrderItemResponse DTO.
     */
    private OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderResponse.OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                toMoneyDto(item.getUnitPrice()),
                item.getQuantity(),
                toMoneyDto(item.calculateLineTotal())
        );
    }
    
    /**
     * Convert AddressDto to domain Address.
     */
    private Address toAddress(AddressDto dto) {
        return new Address(
                dto.street(),
                dto.city(),
                dto.state(),
                dto.zipCode(),
                dto.country()
        );
    }
    
    /**
     * Convert domain Address to AddressDto.
     */
    private AddressDto toAddressDto(Address address) {
        return new AddressDto(
                address.street(),
                address.city(),
                address.state(),
                address.zipCode(),
                address.country()
        );
    }
    
    /**
     * Convert MoneyDto to domain Money.
     */
    private Money toMoney(MoneyDto dto) {
        return Money.of(dto.amount(), dto.currency());
    }
    
    /**
     * Convert domain Money to MoneyDto.
     */
    private MoneyDto toMoneyDto(Money money) {
        return new MoneyDto(money.amount(), money.currency().getCurrencyCode());
    }
}
