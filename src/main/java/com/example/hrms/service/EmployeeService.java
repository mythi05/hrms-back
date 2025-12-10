package com.example.hrms.service;

import com.example.hrms.dto.EmployeeDTO;
import java.util.List;

public interface EmployeeService {

    EmployeeDTO create(EmployeeDTO dto);

    EmployeeDTO update(Long id, EmployeeDTO dto);

    void delete(Long id);

    EmployeeDTO getMe(String username);

    EmployeeDTO getById(Long id);

    EmployeeDTO getByUsername(String username);

    List<EmployeeDTO> getAll();
}
