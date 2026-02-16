package com.example.order_service.client;
import com.example.order_service.dto.CartResponseDto;
import com.example.order_service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
@Component
public class CartServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(CartServiceClient.class);
    private final RestTemplate restTemplate;
    private final String cartServiceBaseUrl;
    public CartServiceClient(RestTemplate restTemplate, @Value("${cart-service.base-url}") String cartServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.cartServiceBaseUrl = cartServiceBaseUrl;
    }
    public CartResponseDto getCart(String authorizationHeader, Long userId, Long tableId) {
        String url = cartServiceBaseUrl;
        logger.info("Fetching cart from Cart Service: {} for userId: {}, tableId: {}", url, userId, tableId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-Table-Id", String.valueOf(tableId));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<CartResponseDto> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, CartResponseDto.class);
            CartResponseDto cart = response.getBody();
            logger.info("Cart fetched - userId: {}, items: {}", cart != null ? cart.getUserId() : null, cart != null && cart.getItems() != null ? cart.getItems().size() : 0);
            return cart;
        } catch (HttpClientErrorException e) {
            logger.error("Cart Service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BadRequestException("Cart not found. Please add items to cart before placing an order.");
            }
            throw new BadRequestException("Failed to fetch cart: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error("Cart Service unavailable: {}", e.getStatusCode());
            throw new BadRequestException("Cart Service is currently unavailable.");
        } catch (Exception e) {
            logger.error("Error fetching cart", e);
            throw new BadRequestException("Failed to fetch cart: " + e.getMessage());
        }
    }
    public void clearCart(String authorizationHeader, Long userId, Long tableId) {
        String url = cartServiceBaseUrl;
        logger.info("Clearing cart via Cart Service: {} for userId: {}, tableId: {}", url, userId, tableId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-Table-Id", String.valueOf(tableId));
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
            logger.info("Cart cleared successfully for userId: {}", userId);
        } catch (Exception e) {
            logger.error("Failed to clear cart (non-critical): {}", e.getMessage());
        }
    }
}

