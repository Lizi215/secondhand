package com.secondhand.product.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.common.constant.Constant;
import com.secondhand.common.exception.BusinessException;
import com.secondhand.common.exception.ErrorCode;
import com.secondhand.common.model.PageResult;
import com.secondhand.product.dto.*;
import com.secondhand.product.entity.Product;
import com.secondhand.product.feign.UserFeignClient;
import com.secondhand.product.mapper.ProductMapper;
import com.secondhand.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate redisTemplate;

    // ======================== Redis 缓存常量 ========================

    // ID 序号
    private static final String REDIS_SEQ_KEY_PREFIX = "product:seq:";

    // 商品详情缓存（5分钟过期）
    private static final String DETAIL_KEY_PREFIX = "product:detail:";
    private static final long DETAIL_CACHE_TTL = 5;

    // 商品列表缓存（15秒过期，版本号控制批量失效）
    private static final String LIST_CACHE_PREFIX = "product:list:";
    private static final long LIST_CACHE_TTL = 15;
    private static final String LIST_CACHE_VERSION_KEY = "product:list:version";
    private static final long LIST_CACHE_PAGE = 1;
    private static final long LIST_CACHE_SIZE = 50;

    // ===============================================================

    @Override
    public ProductDTO addProduct(Long userId, AddProductRequest request) {
        // 参数校验
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_EMPTY);
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_ERROR);
        }

        Product product = new Product();
        product.setProductId(generateProductId());
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setPickupPoint(request.getPickupPoint());
        product.setUserId(userId);
        product.setStatus(Constant.PRODUCT_STATUS_ON);

        // 发布时从 user-service 获取卖家昵称，存入冗余字段
        try {
            com.secondhand.common.model.Result<UserInfoDTO> userResult = userFeignClient.getUserById(userId);
            if (userResult != null && userResult.getData() != null) {
                product.setSellerName(userResult.getData().getNickname());
            }
        } catch (Exception e) {
            log.warn("获取卖家昵称失败，userId={}", userId);
        }

        baseMapper.insert(product);
        log.info("商品上架成功，productId={}, name={}", product.getProductId(), product.getName());

        // 新商品上架 → 商品列表缓存失效
        invalidateListCache();

        return toProductDTO(product);
    }

    /**
     * 生成商品ID：年月日 + 6位序号，如 20260614000001
     * 用 Redis INCR 原子递增，保证并发不重复，TTL 30 天自动过期
     */
    private Long generateProductId() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = REDIS_SEQ_KEY_PREFIX + today;

        try {
            Long seq = redisTemplate.opsForValue().increment(key);
            if (seq == null) {
                throw new RuntimeException("Redis INCR 返回空");
            }
            // 首次创建（seq==1）：与数据库同步，避免和已有数据冲突
            if (seq == 1) {
                try {
                    Long maxId = baseMapper.selectMaxProductId();
                    if (maxId != null && maxId > 0) {
                        String maxStr = String.valueOf(maxId);
                        if (maxStr.startsWith(today)) {
                            long dbSeq = Long.parseLong(maxStr.substring(8));
                            if (dbSeq >= 1) {
                                // 数据库已有今天的数据 → 将 Redis 序号跳到 dbSeq，再 INCR 取下一个
                                redisTemplate.opsForValue().set(key, String.valueOf(dbSeq), 30, TimeUnit.DAYS);
                                seq = redisTemplate.opsForValue().increment(key);
                                return Long.parseLong(today + String.format("%06d", seq));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Redis 同步 DB 序号失败，使用 Redis 自增序号", e);
                }
                // 数据库没有今天的数据 → 正常设置过期时间
                redisTemplate.expire(key, 30, TimeUnit.DAYS);
            }
            return Long.parseLong(today + String.format("%06d", seq));
        } catch (Exception e) {
            log.warn("Redis 不可用，降级到 DB 查询生成 ID: {}", e.getMessage());
            return fallbackGenerateProductId(today);
        }
    }

    /**
     * Redis 不可用时的降级方案：查数据库最大 ID
     */
    private Long fallbackGenerateProductId(String today) {
        Long maxId = baseMapper.selectMaxProductId();
        if (maxId != null && maxId > 0) {
            String maxStr = String.valueOf(maxId);
            if (maxStr.startsWith(today)) {
                long seq = Long.parseLong(maxStr.substring(8)) + 1;
                return Long.parseLong(today + String.format("%06d", seq));
            }
        }
        return Long.parseLong(today + "000001");
    }

    @Override
    public ProductDTO updateProduct(Long userId, UpdateProductRequest request) {
        if (request.getProductId() == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Product product = baseMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_YOURS);
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            product.setName(request.getName().trim());
        }
        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PRODUCT_PRICE_ERROR);
            }
            product.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPickupPoint() != null) {
            product.setPickupPoint(request.getPickupPoint());
        }

        baseMapper.updateById(product);
        log.info("商品修改成功，productId={}", request.getProductId());

        // 商品信息变更 → 详情缓存 + 列表缓存失效
        clearProductDetailCache(request.getProductId());
        invalidateListCache();

        return toProductDTO(product);
    }

    @Override
    public void deleteProduct(Long userId, Long productId) {
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_YOURS);
        }
        baseMapper.deleteById(productId);
        log.info("用户删除商品，productId={}", productId);

        // 删除商品 → 详情缓存 + 列表缓存失效
        clearProductDetailCache(productId);
        invalidateListCache();
    }

    @Override
    public void adminDeleteProduct(Long productId) {
        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        baseMapper.deleteById(productId);
        log.info("管理员删除商品，productId={}", productId);

        // 删除商品 → 详情缓存 + 列表缓存失效
        clearProductDetailCache(productId);
        invalidateListCache();
    }

    @Override
    public List<ProductDTO> getMyProducts(Long userId) {
        List<Product> products = baseMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getUserId, userId)
                        .orderByDesc(Product::getCreatedAt)
        );
        return products.stream().map(this::toProductDTO).collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword, Long excludeUserId) {
        List<Product> products;

        if (keyword == null || keyword.trim().isEmpty()) {
            // 关键词为空 → 取全部上架商品（排除当前用户），按最新发布排序
            products = baseMapper.selectAllByStatusOrderByCreatedAtDesc(Constant.PRODUCT_STATUS_ON);
            // 手动过滤掉当前用户的商品
            if (excludeUserId != null) {
                products = products.stream()
                        .filter(p -> !p.getUserId().equals(excludeUserId))
                        .collect(Collectors.toList());
            }
        } else {
            // 使用手写 SQL 搜索（支持 id 精确匹配 或 名称模糊匹配，排除当前用户）
            products = baseMapper.searchByKeywordExcludeUser(keyword.trim(), excludeUserId);
        }

        return products.stream().map(this::toProductDTO).collect(Collectors.toList());
    }

    @Override
    public PageResult<ProductDTO> listProducts(Long excludeUserId, long page, long size) {
        // 只有第 1 页且是默认大小时走缓存
        if (page == LIST_CACHE_PAGE && size == LIST_CACHE_SIZE) {
            String cacheKey = buildListCacheKey(excludeUserId);
            PageResult<ProductDTO> cached = getCachedProductList(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        Page<Product> mpPage = new Page<>(page, size);
        IPage<Product> result = baseMapper.selectPageByStatus(mpPage, Constant.PRODUCT_STATUS_ON, excludeUserId);

        List<ProductDTO> dtoList = result.getRecords().stream().map(this::toProductDTO).collect(Collectors.toList());

        PageResult<ProductDTO> pageResult = new PageResult<>(dtoList, result.getTotal(), result.getCurrent(), result.getSize());

        // 缓存第 1 页
        if (page == LIST_CACHE_PAGE && size == LIST_CACHE_SIZE) {
            cacheProductList(buildListCacheKey(excludeUserId), pageResult);
        }

        return pageResult;
    }

    @Override
    public ProductDTO getProductDetail(Long productId) {
        // 优先查缓存
        ProductDTO cached = getCachedProductDetail(productId);
        if (cached != null) {
            return cached;
        }

        Product product = baseMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductDTO dto = toProductDTO(product);
        cacheProductDetail(productId, dto);
        return dto;
    }

    @Override
    public List<ProductDTO> getProductsByUserId(Long targetUserId) {
        List<Product> products = baseMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getUserId, targetUserId)
                        .orderByDesc(Product::getCreatedAt)
        );
        return products.stream().map(this::toProductDTO).collect(Collectors.toList());
    }

    /**
     * 将 Product 转为 ProductDTO（sellerName 从实体冗余字段直接获取，无需跨服务调用）
     */
    private ProductDTO toProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        return dto;
    }

    // ======================== 商品详情缓存 ========================

    private ProductDTO getCachedProductDetail(Long productId) {
        try {
            String json = redisTemplate.opsForValue().get(DETAIL_KEY_PREFIX + productId);
            if (json == null) return null;
            return parseProductDTO(JSONUtil.parseObj(json));
        } catch (Exception e) {
            log.warn("Redis 读取商品详情缓存失败", e);
            return null;
        }
    }

    private void cacheProductDetail(Long productId, ProductDTO dto) {
        try {
            redisTemplate.opsForValue().set(
                    DETAIL_KEY_PREFIX + productId,
                    productDTOToJson(dto).toString(),
                    DETAIL_CACHE_TTL,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 写入商品详情缓存失败", e);
        }
    }

    private void clearProductDetailCache(Long productId) {
        try {
            redisTemplate.delete(DETAIL_KEY_PREFIX + productId);
        } catch (Exception e) {
            log.warn("Redis 删除商品详情缓存失败", e);
        }
    }

    // ======================== 商品列表缓存 ========================

    private String buildListCacheKey(Long excludeUserId) {
        String version = "0";
        try {
            String v = redisTemplate.opsForValue().get(LIST_CACHE_VERSION_KEY);
            if (v != null) version = v;
        } catch (Exception ignored) {
        }
        return LIST_CACHE_PREFIX + "page:" + LIST_CACHE_PAGE + ":size:" + LIST_CACHE_SIZE
                + ":user:" + (excludeUserId != null ? excludeUserId : 0) + ":v" + version;
    }

    @SuppressWarnings("unchecked")
    private PageResult<ProductDTO> getCachedProductList(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null) return null;
            JSONObject obj = JSONUtil.parseObj(json);
            long total = obj.getLong("total", 0L);
            long page = obj.getLong("page", 1L);
            long size = obj.getLong("size", LIST_CACHE_SIZE);
            JSONArray records = obj.getJSONArray("records");
            List<ProductDTO> list = new ArrayList<>();
            if (records != null) {
                for (int i = 0; i < records.size(); i++) {
                    list.add(parseProductDTO(records.getJSONObject(i)));
                }
            }
            return new PageResult<>(list, total, page, size);
        } catch (Exception e) {
            log.warn("Redis 读取商品列表缓存失败", e);
            return null;
        }
    }

    private void cacheProductList(String cacheKey, PageResult<ProductDTO> pageResult) {
        try {
            JSONArray records = new JSONArray();
            if (pageResult.getRecords() != null) {
                for (ProductDTO dto : pageResult.getRecords()) {
                    records.add(productDTOToJson(dto));
                }
            }
            JSONObject obj = new JSONObject();
            obj.putOpt("total", pageResult.getTotal());
            obj.putOpt("page", pageResult.getPage());
            obj.putOpt("size", pageResult.getSize());
            obj.putOpt("records", records);
            redisTemplate.opsForValue().set(cacheKey, obj.toString(), LIST_CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 写入商品列表缓存失败", e);
        }
    }

    /**
     * 商品列表缓存失效：递增版本号，所有已缓存的列表页全部作废
     */
    private void invalidateListCache() {
        try {
            redisTemplate.opsForValue().increment(LIST_CACHE_VERSION_KEY);
        } catch (Exception e) {
            log.warn("Redis 失效商品列表缓存失败", e);
        }
    }

    // ======================== JSON 序列化工具 ========================

    /**
     * ProductDTO → JSONObject（日期以 ISO 字符串存储）
     */
    private JSONObject productDTOToJson(ProductDTO dto) {
        JSONObject obj = new JSONObject();
        obj.putOpt("productId", dto.getProductId());
        obj.putOpt("name", dto.getName());
        // 价格用字符串存储以保持两位小数精度
        obj.putOpt("price", dto.getPrice() != null ? dto.getPrice().setScale(2).toPlainString() : null);
        obj.putOpt("description", dto.getDescription());
        obj.putOpt("pickupPoint", dto.getPickupPoint());
        obj.putOpt("userId", dto.getUserId());
        obj.putOpt("sellerName", dto.getSellerName());
        obj.putOpt("status", dto.getStatus());
        obj.putOpt("createdAt", dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : null);
        obj.putOpt("updatedAt", dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : null);
        return obj;
    }

    /**
     * JSONObject → ProductDTO（日期从 ISO 字符串还原）
     */
    private ProductDTO parseProductDTO(JSONObject obj) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(obj.getLong("productId"));
        dto.setName(obj.getStr("name"));
        // 从字符串还原 BigDecimal，保持两位小数
        String priceStr = obj.getStr("price");
        dto.setPrice(priceStr != null ? new BigDecimal(priceStr) : null);
        dto.setDescription(obj.getStr("description"));
        dto.setPickupPoint(obj.getStr("pickupPoint"));
        dto.setUserId(obj.getLong("userId"));
        dto.setSellerName(obj.getStr("sellerName"));
        dto.setStatus(obj.getInt("status"));
        if (obj.getStr("createdAt") != null) {
            dto.setCreatedAt(LocalDateTime.parse(obj.getStr("createdAt")));
        }
        if (obj.getStr("updatedAt") != null) {
            dto.setUpdatedAt(LocalDateTime.parse(obj.getStr("updatedAt")));
        }
        return dto;
    }
}
