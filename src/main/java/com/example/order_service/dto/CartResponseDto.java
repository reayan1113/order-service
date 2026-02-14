package com.example.order_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private Long userId;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
}

