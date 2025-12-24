package com.example.hrms.service;

import com.example.hrms.dto.PerformanceReviewDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.PerformanceReview;
import com.example.hrms.mapper.PerformanceReviewMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceReviewService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;

    public PerformanceReviewDTO getById(Long id) {
        Optional<PerformanceReview> opt = reviewRepository.findById(id);
        if (opt.isEmpty()) return null;
        PerformanceReview r = opt.get();
        Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
        Employee reviewer = r.getReviewerId() != null ? employeeRepository.findById(r.getReviewerId()).orElse(null) : null;
        return PerformanceReviewMapper.toDTO(r, emp, reviewer);
    }

    public List<PerformanceReviewDTO> listByEmployee(Long employeeId) {
        List<PerformanceReview> list = reviewRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        return list.stream().map(r -> {
            Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
            Employee rev = r.getReviewerId() != null ? employeeRepository.findById(r.getReviewerId()).orElse(null) : null;
            return PerformanceReviewMapper.toDTO(r, emp, rev);
        }).collect(Collectors.toList());
    }

    public List<PerformanceReviewDTO> listByReviewer(Long reviewerId) {
        List<PerformanceReview> list = reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId);
        return list.stream().map(r -> {
            Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
            Employee rev = r.getReviewerId() != null ? employeeRepository.findById(r.getReviewerId()).orElse(null) : null;
            return PerformanceReviewMapper.toDTO(r, emp, rev);
        }).collect(Collectors.toList());
    }

    public PerformanceReviewDTO createOrUpdate(PerformanceReviewDTO dto) {
        PerformanceReview entity = PerformanceReviewMapper.toEntity(dto);
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
            if (entity.getStatus() == null) entity.setStatus(PerformanceReview.ReviewStatus.DRAFT);
        }
        entity.setUpdatedAt(now);
        PerformanceReview saved = reviewRepository.save(entity);
        Employee emp = employeeRepository.findById(saved.getEmployeeId()).orElse(null);
        Employee rev = saved.getReviewerId() != null ? employeeRepository.findById(saved.getReviewerId()).orElse(null) : null;
        return PerformanceReviewMapper.toDTO(saved, emp, rev);
    }

    public boolean submit(Long id) {
        Optional<PerformanceReview> opt = reviewRepository.findById(id);
        if (opt.isEmpty()) return false;
        PerformanceReview r = opt.get();
        // chỉ owner (employee) hoặc reviewer có thể submit
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        if (!currentUserId.equals(r.getEmployeeId()) && !currentUserId.equals(r.getReviewerId())) return false;
        r.setStatus(PerformanceReview.ReviewStatus.SUBMITTED);
        r.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(r);
        return true;
    }

    public boolean approve(Long id) {
        Optional<PerformanceReview> opt = reviewRepository.findById(id);
        if (opt.isEmpty()) return false;
        PerformanceReview r = opt.get();
        // chỉ ROLE_ADMIN hoặc ROLE_HR hoặc chính reviewer mới approve
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        boolean isAdminOrHr = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_HR"));
        Long currentUserId = getCurrentUserId();
        if (!isAdminOrHr && !currentUserId.equals(r.getReviewerId())) return false;
        r.setStatus(PerformanceReview.ReviewStatus.APPROVED);
        r.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(r);
        return true;
    }

    public boolean delete(Long id) {
        Optional<PerformanceReview> opt = reviewRepository.findById(id);
        if (opt.isEmpty()) return false;
        PerformanceReview r = opt.get();
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;
        // chỉ admin/hr hoặc reviewer có thể xóa
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrHr = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_HR"));
        if (!isAdminOrHr && !currentUserId.equals(r.getReviewerId())) return false;
        reviewRepository.deleteById(id);
        return true;
    }

    public List<PerformanceReviewDTO> listAllByPeriod(String period) {
        List<PerformanceReview> list = reviewRepository.findAll().stream()
                .filter(r -> period == null || period.isEmpty() || period.equals(r.getPeriod()))
                .sorted((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        return list.stream().map(r -> {
            Employee emp = employeeRepository.findById(r.getEmployeeId()).orElse(null);
            Employee rev = r.getReviewerId() != null ? employeeRepository.findById(r.getReviewerId()).orElse(null) : null;
            return PerformanceReviewMapper.toDTO(r, emp, rev);
        }).collect(Collectors.toList());
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();
        return employeeRepository.findByUsername(username).map(Employee::getId).orElse(null);
    }
}
