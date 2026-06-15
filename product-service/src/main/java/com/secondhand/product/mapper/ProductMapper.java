package com.secondhand.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondhand.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 按关键词搜索商品（支持 id 和 name 模糊匹配），排除指定用户
     */
    List<Product> searchByKeywordExcludeUser(@Param("keyword") String keyword,
                                              @Param("excludeUserId") Long excludeUserId);

    /**
     * 获取全部上架商品，按发布时间倒序
     */
    List<Product> selectAllByStatusOrderByCreatedAtDesc(@Param("status") Integer status);

    /**
     * 获取当前最大商品ID
     */
    Long selectMaxProductId();

    /**
     * 分页查询全部上架商品（排除指定用户），按发布时间倒序
     */
    IPage<Product> selectPageByStatus(Page<?> page,
                                      @Param("status") Integer status,
                                      @Param("excludeUserId") Long excludeUserId);
}
