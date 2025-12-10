package com.example.hrms.repository;

import com.example.hrms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmployeeId(Long employeeId);
    
    Boolean existsByUsername(String username);
    
    void deleteByEmployeeId(Long employeeId);
}
