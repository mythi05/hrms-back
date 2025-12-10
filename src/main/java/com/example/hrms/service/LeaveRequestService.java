package com.example.hrms.service;

import com.example.hrms.dto.LeaveRequestDTO;

import java.util.List;

public interface LeaveRequestService {

    // Employee APIs
    LeaveRequestDTO createRequest(LeaveRequestDTO dto);

    List<LeaveRequestDTO> getMyRequests(Long employeeId);

    // Admin APIs
    List<LeaveRequestDTO> getAllRequests();

    LeaveRequestDTO approveRequest(Long id, Long approverId, String note);

    LeaveRequestDTO rejectRequest(Long id, Long approverId, String rejectReason);
}
