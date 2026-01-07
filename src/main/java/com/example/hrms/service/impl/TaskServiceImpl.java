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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Override
    public TaskDTO createTask(TaskDTO dto, String creatorUsername) {
        Employee creator = employeeRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        Employee assignee = null;
        if (dto.getAssigneeId() != null) {
            assignee = employeeRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Task entity = TaskMapper.toEntity(dto);
        entity.setId(null);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : TaskStatus.NEW);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedById(creator.getId());
        entity.setCreatedByName(creator.getFullName());

        if (assignee != null) {
            entity.setAssigneeId(assignee.getId());
            entity.setAssigneeName(assignee.getFullName());
        }

        Task saved = taskRepository.save(entity);

        // Gửi thông báo cho nhân viên khi được giao việc mới
        if (assignee != null) {
            String title = "Bạn được giao công việc mới";
            String message = "Công việc: " + saved.getTitle() + (saved.getDueDate() != null ? (", hạn hoàn thành: " + saved.getDueDate()) : "");
            notificationService.createNotification(assignee.getId(), title, message, NotificationType.TASK_ASSIGNED);
        }

        return TaskMapper.toDTO(saved);
    }

    @Override
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
        // Gửi thông báo cập nhật công việc cho nhân viên được giao (nếu có)
        if (saved.getAssigneeId() != null) {
            Employee assignee = employeeRepository.findById(saved.getAssigneeId()).orElse(null);
            if (assignee != null) {
                String title = "Công việc của bạn đã được cập nhật";
                String message = "Công việc: " + saved.getTitle() + (saved.getDueDate() != null ? (", hạn hoàn thành: " + saved.getDueDate()) : "");
                notificationService.createNotification(assignee.getId(), title, message, NotificationType.TASK_UPDATED);
            }
        }

        return TaskMapper.toDTO(saved);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getMyTasks(String username) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return getTasksByAssignee(emp.getId());
    }

    @Override
    public TaskDTO updateTaskStatusAsEmployee(Long taskId, String username, String status) {
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!emp.getId().equals(task.getAssigneeId())) {
            throw new IllegalArgumentException("You are not allowed to update this task");
        }

        TaskStatus newStatus = TaskStatus.valueOf(status.toUpperCase());
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        String title = "Cập nhật trạng thái công việc";
        String message = (emp.getFullName() != null ? emp.getFullName() : emp.getUsername())
                + " đã cập nhật công việc: " + saved.getTitle() + " -> " + saved.getStatus();
        try {
            notificationService.createNotificationForAdmins(title, message, NotificationType.TASK_STATUS_UPDATED);
        } catch (Exception ex) {
            // best-effort: do not fail task status update if notification fails
        }

        return TaskMapper.toDTO(saved);
    }
}
