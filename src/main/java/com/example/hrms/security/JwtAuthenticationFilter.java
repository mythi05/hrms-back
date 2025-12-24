package com.example.hrms.security;

import com.example.hrms.entity.Employee;
import com.example.hrms.exception.TokenValidationException;
import com.example.hrms.repository.EmployeeRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    
    private final JwtTokenUtil jwtTokenUtil;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null) {
                if (!jwtTokenUtil.validateToken(jwt)) {
                    log.warn("Invalid JWT token for request {}", request.getRequestURI());
                    handleException(response, "Token không hợp lệ", HttpStatus.UNAUTHORIZED);
                    return;
                }

                String username = jwtTokenUtil.extractUsername(jwt);
                if (username != null) {
                    employeeRepository.findByUsername(username)
                        .ifPresent(user -> {
                            UserDetails userDetails = createUserDetails(user);
                            setAuthentication(userDetails, request);
                        });
                } else {
                    log.warn("JWT token has no subject (username) for request {}", request.getRequestURI());
                }
            } else {
                log.debug("No JWT token found in request {}", request.getRequestURI());
            }
        } catch (ExpiredJwtException ex) {
            log.error("JWT token expired: {}", ex.getMessage());
            handleException(response, "Token đã hết hạn", HttpStatus.UNAUTHORIZED);
            return;
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
            handleException(response, "Token không được hỗ trợ", HttpStatus.BAD_REQUEST);
            return;
        } catch (MalformedJwtException | SignatureException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            handleException(response, "Token không hợp lệ", HttpStatus.BAD_REQUEST);
            return;
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
            handleException(response, "Thông tin xác thực không hợp lệ", HttpStatus.BAD_REQUEST);
            return;
        } catch (Exception ex) {
            log.error("Authentication error: {}", ex.getMessage(), ex);
            handleException(response, "Lỗi xác thực", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails createUserDetails(Employee user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        }

        return new User(
            user.getUsername(),
            user.getPassword(),
            true,
            true,
            true,
            true,
            authorities
        );
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(TOKEN_PREFIX)) {
            return headerAuth.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private void handleException(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}", 
            status.value(), 
            status.getReasonPhrase(), 
            message));
    }
}
