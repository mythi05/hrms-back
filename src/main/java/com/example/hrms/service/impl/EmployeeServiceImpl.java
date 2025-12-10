package com.example.hrms.service.impl;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Skill;
import com.example.hrms.entity.Role;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.EmployeeMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repo;
    private final PasswordEncoder passwordEncoder; // Injected from config

    @Override
    public EmployeeDTO create(EmployeeDTO dto) {
        Employee e = EmployeeMapper.toEntity(dto);
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            e.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        Employee saved = repo.save(e);
        return EmployeeMapper.toDTO(saved);
    }

    @Override
    public EmployeeDTO getMe(String username) {
        Employee emp = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return EmployeeMapper.toDTO(emp);
    }

    @Override
    public EmployeeDTO getByUsername(String username) {
        Employee e = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return EmployeeMapper.toDTO(e);
    }

    @Override
    public EmployeeDTO update(Long id, EmployeeDTO dto) {
        Employee existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Optional.ofNullable(dto.getFullName()).ifPresent(existing::setFullName);
        Optional.ofNullable(dto.getEmail()).ifPresent(existing::setEmail);
        Optional.ofNullable(dto.getPhone()).ifPresent(existing::setPhone);
        Optional.ofNullable(dto.getAddress()).ifPresent(existing::setAddress);
        Optional.ofNullable(dto.getPosition()).ifPresent(existing::setPosition);
        Optional.ofNullable(dto.getDepartment()).ifPresent(existing::setDepartment);
        Optional.ofNullable(dto.getDepartmentId()).ifPresent(existing::setDepartmentId);
        Optional.ofNullable(dto.getStartDate()).ifPresent(existing::setStartDate);
        Optional.ofNullable(dto.getManagerName()).ifPresent(existing::setManagerName);
        Optional.ofNullable(dto.getContractType()).ifPresent(existing::setContractType);
        Optional.ofNullable(dto.getContractEndDate()).ifPresent(existing::setContractEndDate);
        if (dto.getSalary() > 0)
            existing.setSalary(dto.getSalary());

        // Role update safe
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            existing.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        }

        // Password update safe
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // Skills update safe
        if (existing.getSkills() == null)
            existing.setSkills(new ArrayList<>());
        existing.getSkills().clear();
        if (dto.getSkills() != null) {
            dto.getSkills().forEach(s -> {
                Skill skill = EmployeeMapper.toSkill(s);
                skill.setEmployee(existing);
                existing.getSkills().add(skill);
            });
        }

        // Certificates update safe
        if (existing.getCertificates() == null)
            existing.setCertificates(new ArrayList<>());
        existing.getCertificates().clear();
        if (dto.getCertificates() != null) {
            existing.getCertificates().addAll(dto.getCertificates());
        }

        return EmployeeMapper.toDTO(repo.save(existing));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        repo.deleteById(id);
    }

    @Override
    public EmployeeDTO getById(Long id) {
        return repo.findById(id)
                .map(EmployeeMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    @Override
    public List<EmployeeDTO> getAll() {
        return repo.findAll()
                .stream()
                .map(EmployeeMapper::toDTO)
                .toList();
    }
}
