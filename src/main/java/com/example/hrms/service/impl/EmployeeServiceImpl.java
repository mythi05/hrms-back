package com.example.hrms.service.impl;

import com.example.hrms.dto.EmployeeDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.entity.Skill;
import com.example.hrms.entity.Role;
import com.example.hrms.exception.DuplicateEmployeeCodeException;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.EmployeeMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.service.CloudinaryService;
import com.example.hrms.service.EmployeeService;
import com.example.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public EmployeeDTO create(EmployeeDTO dto) {

        String normalizedEmployeeCode = dto.getEmployeeCode() != null ? dto.getEmployeeCode().trim() : null;
        dto.setEmployeeCode(normalizedEmployeeCode);

        // KIỂM TRA TRÙNG MÃ NHÂN VIÊN
        if (normalizedEmployeeCode == null || normalizedEmployeeCode.isBlank()) {
            throw new IllegalArgumentException("Mã nhân viên không được để trống");
        }
        if (repo.existsByEmployeeCode(normalizedEmployeeCode)) {
            throw new DuplicateEmployeeCodeException("Mã nhân viên '" + normalizedEmployeeCode + "' đã tồn tại!");
        }

        Employee e = EmployeeMapper.toEntity(dto);
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            e.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        Employee saved = repo.save(e);

        try {
            String title = "Tạo nhân viên mới";
            String message = "Đã tạo nhân viên" + (saved.getFullName() != null ? (": " + saved.getFullName()) : "")
                    + ", mã: " + saved.getEmployeeCode() + ".";
            notificationService.createNotificationForAdmins(title, message, NotificationType.EMPLOYEE_CREATED);
        } catch (Exception ex) {
            // best-effort
        }

        return EmployeeMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO getMe(String username) {
        Employee emp = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return EmployeeMapper.toDTO(emp);
    }

    @Override
    @Transactional
    public EmployeeDTO updateMe(String username, EmployeeDTO dto) {
        Employee existing = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Optional.ofNullable(dto.getFullName()).ifPresent(existing::setFullName);
        Optional.ofNullable(dto.getEmail()).ifPresent(existing::setEmail);
        Optional.ofNullable(dto.getPhone()).ifPresent(existing::setPhone);
        Optional.ofNullable(dto.getAddress()).ifPresent(existing::setAddress);
        Optional.ofNullable(dto.getAvatar()).ifPresent(existing::setAvatar); // Cho phép user cập nhật avatar

        Optional.ofNullable(dto.getEmailNotifications()).ifPresent(existing::setEmailNotifications);
        Optional.ofNullable(dto.getPushNotifications()).ifPresent(existing::setPushNotifications);
        Optional.ofNullable(dto.getLeaveNotifications()).ifPresent(existing::setLeaveNotifications);
        Optional.ofNullable(dto.getPayrollNotifications()).ifPresent(existing::setPayrollNotifications);

        Optional.ofNullable(dto.getLanguage()).ifPresent(existing::setLanguage);
        Optional.ofNullable(dto.getTheme()).ifPresent(existing::setTheme);
        Optional.ofNullable(dto.getDateFormat()).ifPresent(existing::setDateFormat);

        Employee saved = repo.save(existing);

        try {
            String title = "Cập nhật nhân viên";
            String message = "Đã cập nhật nhân viên" + (saved.getFullName() != null ? (": " + saved.getFullName()) : "")
                    + ", mã: " + saved.getEmployeeCode() + ".";
            notificationService.createNotificationForAdmins(title, message, NotificationType.EMPLOYEE_UPDATED);
        } catch (Exception ex) {
            // best-effort
        }

        return EmployeeMapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO getByUsername(String username) {
        Employee e = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return EmployeeMapper.toDTO(e);
    }

    @Override
    @Transactional
    public EmployeeDTO update(Long id, EmployeeDTO dto) {

        Employee existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // KIỂM TRA TRÙNG USERNAME KHI CẬP NHẬT
        String normalizedUsername = dto.getUsername() != null ? dto.getUsername().trim() : null;
        if (normalizedUsername != null) {
            dto.setUsername(normalizedUsername);
        }
        if (normalizedUsername != null && !normalizedUsername.isBlank()
                && repo.existsByUsernameAndIdNot(normalizedUsername, id)) {
            throw new RuntimeException("Tên đăng nhập '" + normalizedUsername + "' đã tồn tại!");
        }

        // KIỂM TRA TRÙNG MÃ KHI CẬP NHẬT
        String normalizedEmployeeCode = dto.getEmployeeCode() != null ? dto.getEmployeeCode().trim() : null;
        if (normalizedEmployeeCode != null) {
            dto.setEmployeeCode(normalizedEmployeeCode);
        }
        if (normalizedEmployeeCode != null && repo.existsByEmployeeCodeAndIdNot(normalizedEmployeeCode, id)) {
            throw new DuplicateEmployeeCodeException("Mã nhân viên '" + normalizedEmployeeCode + "' đã thuộc về người khác!");
        }

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
        Optional.ofNullable(dto.getEmployeeCode()).ifPresent(existing::setEmployeeCode);
        Optional.ofNullable(dto.getAvatar()).ifPresent(existing::setAvatar); // Cập nhật avatar

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            existing.setUsername(dto.getUsername());
        }

        if (dto.getSalary() > 0)
            existing.setSalary(dto.getSalary());

        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            existing.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        }

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

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

        if (existing.getCertificates() == null)
            existing.setCertificates(new ArrayList<>());
        existing.getCertificates().clear();
        if (dto.getCertificates() != null) {
            existing.getCertificates().addAll(dto.getCertificates());
        }

        Employee saved = repo.save(existing);

        try {
            String title = "Cập nhật nhân viên";
            String message = "Đã cập nhật nhân viên" + (saved.getFullName() != null ? (": " + saved.getFullName()) : "")
                    + ", mã: " + saved.getEmployeeCode() + ".";
            notificationService.createNotificationForAdmins(title, message, NotificationType.EMPLOYEE_UPDATED);
        } catch (Exception ex) {
            // best-effort
        }

        return EmployeeMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeDTO updateAvatar(Long id, MultipartFile file) {
        Employee existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String oldPublicId = existing.getAvatarPublicId();
        CloudinaryService.UploadResult upload = cloudinaryService.uploadImage(file, "hrms/avatars");
        existing.setAvatar(upload.url());
        existing.setAvatarPublicId(upload.publicId());

        Employee saved = repo.save(existing);

        try {
            cloudinaryService.deleteByPublicId(oldPublicId);
        } catch (Exception ex) {
            // best-effort
        }

        try {
            String title = "Cập nhật ảnh nhân viên";
            String message = "Đã cập nhật ảnh đại diện cho " + (saved.getFullName() != null ? saved.getFullName() : saved.getUsername())
                    + " (" + saved.getEmployeeCode() + ")";
            notificationService.createNotificationForAdmins(title, message, NotificationType.EMPLOYEE_AVATAR_UPDATED);
        } catch (Exception ex) {
            // best-effort
        }

        return EmployeeMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeDTO updateMyAvatar(String username, MultipartFile file) {
        Employee existing = repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String oldPublicId = existing.getAvatarPublicId();
        CloudinaryService.UploadResult upload = cloudinaryService.uploadImage(file, "hrms/avatars");
        existing.setAvatar(upload.url());
        existing.setAvatarPublicId(upload.publicId());

        Employee saved = repo.save(existing);

        try {
            cloudinaryService.deleteByPublicId(oldPublicId);
        } catch (Exception ex) {
            // best-effort
        }

        try {
            String title = "Ảnh đại diện đã được cập nhật";
            String message = "Bạn đã cập nhật ảnh đại diện của mình.";
            notificationService.createNotification(saved.getId(), title, message, NotificationType.EMPLOYEE_AVATAR_UPDATED);
        } catch (Exception ex) {
            // best-effort
        }

        return EmployeeMapper.toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO getById(Long id) {
        return repo.findById(id)
                .map(EmployeeMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAll() {
        return repo.findAll()
                .stream()
                .map(EmployeeMapper::toDTO)
                .collect(Collectors.toList());
    }
}