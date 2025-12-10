package com.example.hrms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenUtil {

    // Có thể config trong application.properties để dễ thay đổi
    private final String SECRET_KEY = "mythijwtsecretkeymythijwtsecretkey";
    private final long EXPIRATION_TIME = 86400000; // 1 ngày

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // ✅ Tạo token với username + employeeId + roles
    public String generateToken(String username, Long employeeId, String role) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .addClaims(Map.of(
                        "employeeId", employeeId,
                        "role", role
                ))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Lấy username từ token
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // ✅ Lấy employeeId từ token
    public Long extractEmployeeId(String token) {
        Object id = getClaims(token).get("employeeId");
        return id != null ? Long.valueOf(id.toString()) : null;
    }

    // ✅ Lấy role từ token
    public String extractRole(String token) {
        Object role = getClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    // ✅ Kiểm tra token hợp lệ
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            System.out.println("❌ Token invalid: " + e.getMessage());
            return false;
        }
    }

    // ✅ Lấy claims chung
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
