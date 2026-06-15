package com.secondhand.gateway.filter;

import com.secondhand.common.constant.Constant;
import com.secondhand.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 鉴权全局过滤器
 * <p>
 * 白名单路径跳过鉴权；非白名单路径校验 Token，解析后将用户信息放入请求头。
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 白名单路径（无需登录即可访问）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/auth/login",
            "/auth/register"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单路径直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 获取 Authorization 头
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(Constant.AUTH_HEADER);

        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("缺少 Token，请求路径：{}", path);
            return unauthorized(exchange.getResponse(), "缺少Token");
        }

        String token = JwtUtils.extractToken(authHeader);
        if (token == null || !JwtUtils.validateToken(token)) {
            log.warn("Token 无效或已过期，请求路径：{}", path);
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        // 解析用户信息
        Long userId;
        Integer role;
        try {
            userId = JwtUtils.getUserId(token);
            role = JwtUtils.getRole(token);
        } catch (Exception e) {
            log.error("Token 解析失败", e);
            return unauthorized(exchange.getResponse(), "Token解析失败");
        }

        // 将用户信息放入请求头传递给下游服务
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(Constant.USER_ID_HEADER, String.valueOf(userId))
                .header(Constant.USER_ROLE_HEADER, String.valueOf(role))
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断是否在白名单路径中
     */
    private boolean isWhiteList(String path) {
        for (String pattern : WHITE_LIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        String body = "{\"code\":1001,\"msg\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
