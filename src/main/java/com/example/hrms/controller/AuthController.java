package com.example.hrms.controller;

import com.example.hrms.entity.Employee;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    // ========================= LOGIN ============================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing username or password"));
        }

        Optional<Employee> empOpt = employeeRepository.findByUsername(username);

        // ❌ Không tìm thấy tài khoản hoặc sai mật khẩu
        if (empOpt.isEmpty() || !passwordEncoder.matches(password, empOpt.get().getPassword())) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid username or password"));
        }

        Employee emp = empOpt.get();

        // 🟩 Spring Security yêu cầu ROLE_ prefix
        String token = jwtTokenUtil.generateToken(
                emp.getUsername(),
                emp.getId(),
                "ROLE_" + emp.getRole().name()
        );

        // Data trả về JSON
        Map<String, Object> userData = Map.of(
                "id", emp.getId(),
                "username", emp.getUsername(),
                "fullName", emp.getFullName(),
                "email", emp.getEmail(),
                "role", emp.getRole().name()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("user", userData);

        return ResponseEntity.ok(response);
    }

    // ========================= LOGOUT ============================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        // Nếu muốn có blacklist token thì thêm vào đây
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    // ========================= INIT ADMIN ============================
    @PostMapping("/init")
    public ResponseEntity<?> initAdmin() {

        // Nếu admin1 đã tồn tại -> báo lỗi
        if (employeeRepository.findByUsername("admin1").isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admin already exists"));
        }

        // Tạo admin mặc định
        Employee admin = Employee.builder()
                .username("admin1")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Administrator")
                .email("admin@example.com")
                .role(com.example.hrms.entity.Role.ADMIN)
                .salary(0)
                .build();

        employeeRepository.save(admin);

        return ResponseEntity.ok(Map.of("message", "Admin account created successfully"));
    }
}
