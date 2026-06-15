package com.secondhand.product.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 上架商品请求 DTO
 */
@Data
public class AddProductRequest {

    /** 商品名称（必填） */
    private String name;

    /** 价格（必填） */
    private BigDecimal price;

    /** 介绍 */
    private String description;

    /** 自提点 */
    private String pickupPoint;
}
