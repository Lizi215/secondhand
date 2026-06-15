package com.secondhand.product.controller;

import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.model.PageResult;
import com.secondhand.common.model.Result;
import com.secondhand.product.dto.AddProductRequest;
import com.secondhand.product.dto.ProductDTO;
import com.secondhand.product.dto.UpdateProductRequest;
import com.secondhand.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 上架商品
     */
    @PostMapping("/add")
    public Result<ProductDTO> addProduct(@RequestBody AddProductRequest request,
                                         HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(productService.addProduct(userId, request));
    }

    /**
     * 修改商品
     */
    @PutMapping("/update")
    public Result<ProductDTO> updateProduct(@RequestBody UpdateProductRequest request,
                                            HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(productService.updateProduct(userId, request));
    }

    /**
     * 删除自己的商品
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteProduct(@PathVariable("id") Long productId,
                                      HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        productService.deleteProduct(userId, productId);
        return Result.success();
    }

    /**
     * 获取我的商品列表
     */
    @GetMapping("/my")
    public Result<List<ProductDTO>> getMyProducts(HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(productService.getMyProducts(userId));
    }

    /**
     * 搜索商品（支持商品 ID 或名称搜索，排除当前用户自己的商品）
     */
    @GetMapping("/search")
    public Result<List<ProductDTO>> searchProducts(@RequestParam("keyword") String keyword,
                                                    HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(productService.searchProducts(keyword, userId));
    }

    /**
     * 分页获取商品列表（排除自己的商品），按发布时间倒序
     */
    @GetMapping("/list")
    public Result<PageResult<ProductDTO>> listProducts(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "50") long size,
            HttpServletRequest servletRequest) {
        Long userId = getUserId(servletRequest);
        return Result.success(productService.listProducts(userId, page, size));
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDTO> getProductDetail(@PathVariable("id") Long productId) {
        return Result.success(productService.getProductDetail(productId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员删除任意商品
     */
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> adminDeleteProduct(@PathVariable("id") Long productId,
                                           HttpServletRequest servletRequest) {
        checkAdmin(servletRequest);
        productService.adminDeleteProduct(productId);
        return Result.success();
    }

    /**
     * 管理员查看指定用户的所有商品
     */
    @GetMapping("/admin/user-products/{userId}")
    public Result<List<ProductDTO>> getUserProducts(@PathVariable("userId") Long targetUserId,
                                                     HttpServletRequest servletRequest) {
        checkAdmin(servletRequest);
        return Result.success(productService.getProductsByUserId(targetUserId));
    }

    // ==================== 内部辅助 ====================

    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(Constant.USER_ID_HEADER);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.parseLong(userIdStr);
    }

    private void checkAdmin(HttpServletRequest request) {
        String roleStr = request.getHeader(Constant.USER_ROLE_HEADER);
        if (roleStr == null || Integer.parseInt(roleStr) != Constant.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
