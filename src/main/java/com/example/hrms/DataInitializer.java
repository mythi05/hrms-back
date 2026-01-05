package com.example.hrms;

import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import com.example.hrms.entity.User;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminUser() {
        return args -> {
            log.info("=== DataInitializer started ===");
            try {
                String adminUsername = "admin";
                String adminPassword = "admin123"; // Match frontend login form

                log.info("Checking if admin user exists...");
                // Check if admin already exists in Employee table (AuthController uses EmployeeRepository)
                if (employeeRepository.findByUsername(adminUsername).isPresent()) {
                    log.info("Admin user '{}' đã tồn tại, bỏ qua khởi tạo.", adminUsername);
                    return;
                }

                log.info("Creating new admin user...");
                // Create employee for admin (this is what AuthController uses for login)
                Employee adminEmp = Employee.builder()
                        .fullName("System Administrator")
                        .email("admin@example.com")
                        .phone("0123456789")
                        .startDate(LocalDate.now())
                        .department("IT")
                        .position("ADMIN")
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .build();

                adminEmp = employeeRepository.save(adminEmp);
                log.info("Đã khởi tạo tài khoản admin mặc định: username='{}', password='{}'", adminUsername, adminPassword);
                log.info("=== DataInitializer completed ===");
            } catch (Exception ex) {
                log.error("ERROR in DataInitializer: {}", ex.getMessage(), ex);
                throw new RuntimeException("DataInitializer failed", ex);
            }
        };
    }
}
