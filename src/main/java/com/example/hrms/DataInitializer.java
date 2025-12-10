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
            String adminUsername = "admin";

            if (userRepository.existsByUsername(adminUsername)) {
                log.info("Admin user '{}' đã tồn tại, bỏ qua khởi tạo.", adminUsername);
                return;
            }

            // Tạo employee cho admin
            Employee adminEmp = Employee.builder()
                    .fullName("System Administrator")
                    .email("admin@example.com")
                    .phone("0123456789")
                    .startDate(LocalDate.now())
                    .department("IT")
                    .position("ADMIN")
                    .username(adminUsername)
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();

            adminEmp = employeeRepository.save(adminEmp);

            // Tạo user cho admin, link với employee
            User adminUser = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .employeeId(adminEmp.getId())
                    .isActive(true)
                    .build();

            userRepository.save(adminUser);

            log.info("Đã khởi tạo tài khoản admin mặc định: username='{}', password='admin123'", adminUsername);
        };
    }
}
