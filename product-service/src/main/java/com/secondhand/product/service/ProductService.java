package com.secondhand.product.service;

import com.secondhand.common.model.PageResult;
import com.secondhand.product.dto.AddProductRequest;
import com.secondhand.product.dto.ProductDTO;
import com.secondhand.product.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    /**
     * 上架商品
     */
    ProductDTO addProduct(Long userId, AddProductRequest request);

    /**
     * 修改商品
     */
    ProductDTO updateProduct(Long userId, UpdateProductRequest request);

    /**
     * 删除自己的商品
     */
    void deleteProduct(Long userId, Long productId);

    /**
     * 管理员删除商品
     */
    void adminDeleteProduct(Long productId);

    /**
     * 获取我的商品列表
     */
    List<ProductDTO> getMyProducts(Long userId);

    /**
     * 搜索商品（按关键词，支持 id/名称），排除当前用户自己的商品
     */
    List<ProductDTO> searchProducts(String keyword, Long excludeUserId);

    /**
     * 获取指定用户的所有商品（管理员用）
     */
    List<ProductDTO> getProductsByUserId(Long targetUserId);

    /**
     * 分页获取商品列表（排除当前用户的商品），按发布时间倒序
     */
    PageResult<ProductDTO> listProducts(Long excludeUserId, long page, long size);

    /**
     * 获取商品详情
     */
    ProductDTO getProductDetail(Long productId);
}
