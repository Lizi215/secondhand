package com.secondhand.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改商品请求 DTO
 */
@Data
public class UpdateProductRequest {

    private Long productId;
    private String name;
    private BigDecimal price;
    private String description;
    private String pickupPoint;
}
