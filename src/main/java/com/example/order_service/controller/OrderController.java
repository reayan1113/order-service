package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.UpdateOrderStatusRequest;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Table-Id") Long tableId,
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) CreateOrderRequest request) {
        logger.info("POST /api/orders - Creating order for userId: {}, tableId: {}", userId, tableId);

        // Create request object if not provided (body is optional)
        if (request == null) {
            request = new CreateOrderRequest();
        }

        request.setUserId(userId);
        request.setTableId(tableId);
        OrderResponse response = orderService.createOrder(request, authorization);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        logger.info("GET /api/orders/{} - Fetching order", orderId);
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/table")
    public ResponseEntity<List<OrderResponse>> getOrdersByTable(@RequestHeader("X-Table-Id") Long tableId) {
        logger.info("GET /api/orders/table - Fetching orders by table: {}", tableId);
        List<OrderResponse> responses = orderService.getOrdersByTable(tableId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(@RequestHeader("X-User-Id") Long userId) {
        logger.info("GET /api/orders/user - Fetching orders by user: {}", userId);
        List<OrderResponse> responses = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders() {
        logger.info("GET /api/orders/active - Fetching active orders");
        List<OrderResponse> responses = orderService.getActiveOrders();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        logger.info("PATCH /api/orders/{}/status - Updating status to: {}", orderId, request.getStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(response);
    }
}

