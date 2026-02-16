package com.example.order_service.service;

import com.example.order_service.client.CartServiceClient;
import com.example.order_service.dto.CartItemDto;
import com.example.order_service.dto.CartResponseDto;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.OrderItemRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.exception.BadRequestException;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;

    public OrderService(OrderRepository orderRepository, CartServiceClient cartServiceClient) {
        this.orderRepository = orderRepository;
        this.cartServiceClient = cartServiceClient;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String authorizationHeader) {

        if (request.getUserId() == null) {
            logger.error("User ID is missing in the request");
            throw new BadRequestException("User ID is required. Please provide X-User-Id header.");
        }

        if (request.getTableId() == null) {
            logger.error("Table ID is missing in the request");
            throw new BadRequestException("Table ID is required. Please provide X-Table-Id header.");
        }

        logger.info("Creating new order for userId: {}, tableId: {}", request.getUserId(), request.getTableId());

        List<OrderItem> orderItems = new java.util.ArrayList<>();

        // Step 1: Check if items are provided in the request body
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            logger.info("Using items provided in the request body (itemCount: {})", request.getItems().size());

            // Validate and map items from request
            for (OrderItemRequest itemReq : request.getItems()) {
                if (itemReq.getItemId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                    logger.error("Invalid item in request: {}", itemReq);
                    throw new BadRequestException(
                            "Request contains invalid items. Item ID and positive quantity are required.");
                }
                if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().signum() <= 0) {
                    logger.error("Invalid price for item in request: {}", itemReq);
                    throw new BadRequestException("Request contains items with invalid prices.");
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setItemId(itemReq.getItemId());
                orderItem.setItemName(itemReq.getItemName());
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setUnitPrice(itemReq.getUnitPrice());
                orderItems.add(orderItem);
            }
        } else {
            // Step 2: Fallback - Fetch cart from Cart Service
            logger.info("Request items empty, fetching cart for userId: {}", request.getUserId());
            CartResponseDto cart = cartServiceClient.getCart(authorizationHeader, request.getUserId(),
                    request.getTableId());

            if (cart == null) {
                logger.error("Cart Service returned null cart");
                throw new BadRequestException("Failed to fetch cart. Please try again.");
            }

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                logger.error("Cart is empty for userId: {}", request.getUserId());
                throw new BadRequestException("Cart is empty. Please add items to cart before placing an order.");
            }

            // Validate and map cart items
            for (CartItemDto cartItem : cart.getItems()) {
                if (cartItem.getItemId() == null || cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                    logger.error("Invalid cart item: {}", cartItem);
                    throw new BadRequestException("Cart contains invalid items. Please review your cart.");
                }
                if (cartItem.getUnitPrice() == null || cartItem.getUnitPrice().signum() <= 0) {
                    logger.error("Invalid price for cart item: {}", cartItem);
                    throw new BadRequestException("Cart contains items with invalid prices.");
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setItemId(cartItem.getItemId());
                orderItem.setItemName(cartItem.getItemName());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(cartItem.getUnitPrice());
                orderItems.add(orderItem);
            }

            logger.info("Cart validation successful - itemCount: {}, totalAmount: {}",
                    cart.getItems().size(), cart.getTotalAmount());
        }

        // Step 3: Create and save order entity
        Order order = new Order();
        order.setTableId(request.getTableId());
        order.setUserId(request.getUserId());
        order.setStatus(Order.OrderStatus.CREATED);

        // Add items to order
        orderItems.forEach(order::addItem);

        // Calculate total amount
        order.calculateTotalAmount();

        // Save order
        Order savedOrder = orderRepository.save(order);

        logger.info("Order created successfully - orderId: {}, userId: {}, totalAmount: {}",
                savedOrder.getId(), savedOrder.getUserId(), savedOrder.getTotalAmount());

        // Step 4: Clear cart after successful order creation
        try {
            logger.info("Clearing cart for userId: {} after successful order creation", request.getUserId());
            cartServiceClient.clearCart(authorizationHeader, request.getUserId(), request.getTableId());
        } catch (Exception e) {
            logger.warn("Failed to clear cart after order creation (orderId: {}): {}",
                    savedOrder.getId(), e.getMessage());
        }

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        logger.info("Fetching order by id: {}", orderId);

        Order order = orderRepository.findByIdWithItems(orderId);
        if (order == null) {
            logger.error("Order not found with id: {}", orderId);
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        logger.debug("Order found - orderId: {}, userId: {}", order.getId(), order.getUserId());
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTable(Long tableId) {
        logger.info("Fetching orders for tableId: {}", tableId);

        List<Order> orders = orderRepository.findByTableIdOrderByCreatedAtDesc(tableId);
        logger.debug("Found {} orders for tableId: {}", orders.size(), tableId);

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        logger.info("Fetching orders for userId: {}", userId);

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.debug("Found {} orders for userId: {}", orders.size(), userId);

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders() {
        logger.info("Fetching active orders");

        List<Order> orders = orderRepository.findActiveOrders();
        logger.debug("Found {} active orders", orders.size());

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        logger.info("Updating order status - orderId: {}, newStatus: {}", orderId, newStatus);

        Order order = orderRepository.findByIdWithItems(orderId);
        if (order == null) {
            logger.error("Order not found with id: {}", orderId);
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        Order.OrderStatus oldStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        logger.info("Order status updated successfully - orderId: {}, userId: {}, {} -> {}",
                orderId, order.getUserId(), oldStatus, newStatus);

        return OrderResponse.fromEntity(updatedOrder);
    }

    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // Define allowed transitions
        boolean isValidTransition = switch (currentStatus) {
            case CREATED -> Arrays.asList(Order.OrderStatus.CONFIRMED, Order.OrderStatus.PREPARING).contains(newStatus);
            case CONFIRMED -> Arrays.asList(Order.OrderStatus.PREPARING).contains(newStatus);
            case PREPARING -> Arrays.asList(Order.OrderStatus.READY).contains(newStatus);
            case READY -> Arrays.asList(Order.OrderStatus.SERVED).contains(newStatus);
            case SERVED -> false; // No transitions from SERVED
        };

        if (!isValidTransition) {
            String message = String.format("Invalid status transition from %s to %s", currentStatus, newStatus);
            logger.error(message);
            throw new BadRequestException(message);
        }
    }
}
