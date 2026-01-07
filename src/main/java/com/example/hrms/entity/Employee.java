package com.example.hrms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "skills"})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String address;
    private String avatarUrl;
    private String position;
    private String department;
    @Column(name = "department_id")
    private Long departmentId;
    private LocalDate startDate;
    private String managerName;
    private String contractType;
    private LocalDate contractEndDate;
    private int experienceYears;
    private String grade;
    private int performanceRate;
    private String employeeCode;
    private double salary;

    @Column(unique = true)
    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    private Boolean emailNotifications = true;

    @Builder.Default
    private Boolean pushNotifications = true;

    @Builder.Default
    private Boolean leaveNotifications = true;

    @Builder.Default
    private Boolean payrollNotifications = true;

    @Builder.Default
    private String language = "vi";

    @Builder.Default
    private String theme = "light";

    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();

    @ElementCollection
    @Builder.Default
    private List<String> certificates = new ArrayList<>();
}
