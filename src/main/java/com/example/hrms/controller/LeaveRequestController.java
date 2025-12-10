package com.example.hrms.controller;

import com.example.hrms.dto.LeaveRequestDTO;
import com.example.hrms.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LeaveRequestController {

    private final LeaveRequestService leaveService;

    // ==== EMPLOYEE APIs ====

    @PostMapping
    public ResponseEntity<LeaveRequestDTO> create(@RequestBody LeaveRequestDTO dto) {
        // Frontend cần truyền employeeId tương ứng nhân viên đang login
        return ResponseEntity.ok(leaveService.createRequest(dto));
    }

    @GetMapping("/my/{employeeId}")
    public ResponseEntity<List<LeaveRequestDTO>> getMyRequests(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getMyRequests(employeeId));
    }

    // ==== ADMIN APIs ====

    @GetMapping("/admin/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().toString();
        
        return ResponseEntity.ok(Map.of(
            "username", username,
            "authorities", authorities,
            "principalClass", principal.getClass().getSimpleName()
        ));
    }

    @GetMapping("/admin/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Admin access OK");
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<LeaveRequestDTO>> getAll() {
        return ResponseEntity.ok(leaveService.getAllRequests());
    }

    @PostMapping("/admin/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LeaveRequestDTO> approve(
            @PathVariable Long id,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(leaveService.approveRequest(id, approverId, note));
    }

    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LeaveRequestDTO> reject(
            @PathVariable Long id,
            @RequestParam(required = false) Long approverId,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(leaveService.rejectRequest(id, approverId, reason));
    }
}
