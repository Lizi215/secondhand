package com.secondhand.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品展示 DTO
 */
@Data
public class ProductDTO {

    private Long productId;
    private String name;
    private BigDecimal price;
    private String description;
    private String pickupPoint;
    private Long userId;
    private String sellerName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
