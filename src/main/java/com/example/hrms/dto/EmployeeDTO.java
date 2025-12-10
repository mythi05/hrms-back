package com.example.hrms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {

    private Long id;
    private String fullName;
    private String email;
    private String phone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String address;
    private String position;
    private String department;
    private Long departmentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private String managerName;
    private String contractType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate contractEndDate;

    private int experienceYears;
    private String grade;
    private int performanceRate;
    private String employeeCode;
    private double salary;

    private List<SkillDTO> skills;
    private List<String> certificates;

    private String username;
    private String password;
    private String role; // ADMIN, HR, EMPLOYEE
}
