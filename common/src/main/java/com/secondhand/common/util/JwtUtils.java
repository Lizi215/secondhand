package com.secondhand.common.util;

import com.secondhand.common.constant.Constant;
import io.jsonwebtoken.*;

import java.util.Date;

/**
 * JWT 工具类
 */
public class JwtUtils {

    /**
     * 生成 Token
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return JWT Token 字符串
     */
    public static String generateToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + Constant.JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, Constant.JWT_SECRET)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(Constant.JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public static Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 从 Token 中获取用户角色
     */
    public static Integer getRole(String token) {
        return (Integer) parseToken(token).get("role");
    }

    /**
     * 校验 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从请求头中提取 Token（去掉 Bearer 前缀）
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(Constant.TOKEN_PREFIX)) {
            return authHeader.substring(Constant.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
