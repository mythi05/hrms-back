package com.example.hrms.mapper;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.dto.SkillDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Role;
import com.example.hrms.entity.Skill;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeMapper {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // DTO → Entity
    public static Employee toEntity(EmployeeDTO dto) {
        List<Skill> skills = new ArrayList<>();
        if (dto.getSkills() != null) {
            skills = dto.getSkills().stream()
                    .map(EmployeeMapper::toSkill)
                    .collect(Collectors.toList());
        }

        Employee e = Employee.builder()
                .id(dto.getId())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .dob(dto.getDob())
                .address(dto.getAddress())
                .position(dto.getPosition())
                .department(dto.getDepartment())
                .departmentId(dto.getDepartmentId())
                .startDate(dto.getStartDate())
                .managerName(dto.getManagerName())
                .contractType(dto.getContractType())
                .contractEndDate(dto.getContractEndDate())
                .experienceYears(dto.getExperienceYears())
                .grade(dto.getGrade())
                .performanceRate(dto.getPerformanceRate())
                .employeeCode(dto.getEmployeeCode())
                .salary(dto.getSalary())
                .username(dto.getUsername())
                .password(dto.getPassword() != null ? passwordEncoder.encode(dto.getPassword()) : null)
                .role(dto.getRole() != null ? Role.valueOf(dto.getRole().toUpperCase()) : Role.EMPLOYEE)
                .skills(skills)
                .certificates(dto.getCertificates() != null ? dto.getCertificates() : new ArrayList<>())
                .build();

        // Set employee cho mỗi skill
        skills.forEach(skill -> skill.setEmployee(e));

        return e;
    }

    // Entity → DTO
    public static EmployeeDTO toDTO(Employee e) {
        List<SkillDTO> skillDTOs = new ArrayList<>();
        if (e.getSkills() != null) {
            skillDTOs = e.getSkills().stream()
                    .map(s -> SkillDTO.builder()
                            .name(s.getName())
                            .level(String.valueOf(s.getLevel()))
                            .build())
                    .collect(Collectors.toList());
        }

        List<String> certificates = new ArrayList<>();
        if (e.getCertificates() != null) {
            certificates.addAll(e.getCertificates());
        }

        return EmployeeDTO.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .dob(e.getDob())
                .address(e.getAddress())
                .position(e.getPosition())
                .department(e.getDepartment())
                .departmentId(e.getDepartmentId())
                .startDate(e.getStartDate())
                .managerName(e.getManagerName())
                .contractType(e.getContractType())
                .contractEndDate(e.getContractEndDate())
                .experienceYears(e.getExperienceYears())
                .grade(e.getGrade())
                .performanceRate(e.getPerformanceRate())
                .employeeCode(e.getEmployeeCode())
                .salary(e.getSalary())
                .skills(skillDTOs)
                .certificates(certificates)
                .username(e.getUsername())
                .role(e.getRole() != null ? e.getRole().name() : null)
                .build();
    }

    // SkillDTO → Skill
    public static Skill toSkill(SkillDTO dto) {
        Skill skill = new Skill();
        skill.setName(dto.getName());
        skill.setLevel(Integer.parseInt(dto.getLevel()));
        return skill;
    }

    // Skill → SkillDTO
    public static SkillDTO toSkillDTO(Skill skill) {
        return SkillDTO.builder()
                .name(skill.getName())
                .level(String.valueOf(skill.getLevel()))
                .build();
    }
}
