package com.example.hrms.security;

import com.example.hrms.entity.Employee;
import com.example.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee emp = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = "ROLE_" + emp.getRole().name();  // Spring requires prefix ROLE_

        return new User(
                emp.getUsername(),
                emp.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
