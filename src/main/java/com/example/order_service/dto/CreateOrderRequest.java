package com.example.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    // These will be set from headers by the controller
    private Long tableId;
    private Long userId;

    // Items are now fetched from Cart Service, so this field is optional
    // Kept for backward compatibility or direct order creation (future use)
    @Valid
    private List<OrderItemRequest> items;
}


