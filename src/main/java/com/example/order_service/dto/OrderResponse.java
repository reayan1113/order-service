package com.example.order_service.dto;

import com.example.order_service.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long tableId;
    private Long userId;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTableId(order.getTableId());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList()));
        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;

        public static OrderItemResponse fromEntity(com.example.order_service.entity.OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getItemId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnitPrice()
            );
        }
    }
}

