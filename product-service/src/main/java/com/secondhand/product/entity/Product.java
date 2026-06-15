package com.secondhand.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.INPUT)
    private Long productId;

    private String name;

    private BigDecimal price;

    private String description;

    /** 自提点 */
    private String pickupPoint;

    /** 商家用户ID */
    private Long userId;

    /** 卖家昵称（冗余存储，避免跨服务查询） */
    private String sellerName;

    /** 1:上架, 0:下架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
