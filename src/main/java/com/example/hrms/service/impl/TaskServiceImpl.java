package com.example.hrms.service.impl;

import com.example.hrms.dto.TaskDTO;
import com.example.hrms.entity.Employee;
import com.example.hrms.entity.Task;
import com.example.hrms.entity.Task.TaskStatus;
import com.example.hrms.entity.Notification.NotificationType;
import com.example.hrms.exception.ResourceNotFoundException;
import com.example.hrms.mapper.TaskMapper;
import com.example.hrms.repository.EmployeeRepository;
import com.example.hrms.repository.TaskRepository;
import com.example.hrms.service.NotificationService;
import com.example.hrms.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    // =========================
    // ADMIN / HR
    // =========================

    @Override
    @Transactional
    public TaskDTO createTask(TaskDTO dto, String creatorUsername) {
        Employee creator = employeeRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        Employee assignee = null;
        if (dto.getAssigneeId() != null) {
            assignee = employeeRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Task task = TaskMapper.toEntity(dto);
        task.setId(null);
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : TaskStatus.NEW);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCreatedById(creator.getId());
        task.setCreatedByName(creator.getFullName());

        if (assignee != null) {
            task.setAssigneeId(assignee.getId());
            task.setAssigneeName(assignee.getFullName());
        }

        Task saved = taskRepository.save(task);

        // gửi notification SAU KHI save (best-effort)
        if (assignee != null) {
            try {
                String title = "Bạn được giao công việc mới";
                String message = "Công việc: " + saved.getTitle()
                        + (saved.getDueDate() != null ? ", hạn: " + saved.getDueDate() : "");
                notificationService.createNotification(
                        assignee.getId(),
                        title,
                        message,
                        NotificationType.TASK_ASSIGNED
                );
            } catch (Exception ex) {
                log.warn("Notification failed (createTask): {}", ex.getMessage());
            }
        }

        return TaskMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getDueDate() != null) task.setDueDate(dto.getDueDate());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());

        if (dto.getAssigneeId() != null) {
            Employee assignee = employeeRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssigneeId(assignee.getId());
            task.setAssigneeName(assignee.getFullName());
        }

        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // notification best-effort
        if (saved.getAssigneeId() != null) {
            try {
                String title = "Công việc của bạn đã được cập nhật";
                String message = "Công việc: " + saved.getTitle();
                notificationService.createNotification(
                        saved.getAssigneeId(),
                        title,
                        message,
                        NotificationType.TASK_UPDATED
                );
            } catch (Exception ex) {
                log.warn("Notification failed (updateTask): {}", ex.getMessage());
            }
        }

        return TaskMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found");
        }
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssignee(Long employeeId) {
        return taskRepository.findByAssigneeIdOrderByDueDateAsc(employeeId).stream()
                .map(TaskMapper::toDTO)
                .collect(Collectors.toList());
    }

    // =========================
    // EMPLOYEE
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getMyTasks(String username) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return getTasksByAssignee(emp.getId());
    }

    @Override
    @Transactional
    public TaskDTO updateTaskStatusAsEmployee(Long taskId, String username, String status) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (task.getAssigneeId() == null || !emp.getId().equals(task.getAssigneeId())) {
            throw new IllegalArgumentException("You are not allowed to update this task");
        }

        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid status. Allowed: NEW, IN_PROGRESS, COMPLETED, CANCELLED"
            );
        }

        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // notify admin (best-effort)
        try {
            String title = "Cập nhật trạng thái công việc";
            String message = emp.getFullName() + " cập nhật: "
                    + saved.getTitle() + " → " + saved.getStatus();
            notificationService.createNotificationForAdmins(
                    title,
                    message,
                    NotificationType.TASK_STATUS_UPDATED
            );
        } catch (Exception ex) {
            log.warn("Notification failed (updateTaskStatus): {}", ex.getMessage());
        }

        return TaskMapper.toDTO(saved);
    }
}
