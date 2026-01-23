package com.example.hrms.controller;

import com.example.hrms.dto.TaskDTO;
import com.example.hrms.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // --- Admin/HR: create task and assign to employee ---
    @PostMapping("/admin/tasks")
    public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO dto, Authentication authentication) {
        String username = authentication.getName();
        TaskDTO created = taskService.createTask(dto, username);
        return ResponseEntity.ok(created);
    }

    // --- Admin/HR: update task ---
    @PutMapping("/admin/tasks/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @RequestBody TaskDTO dto) {
        TaskDTO updated = taskService.updateTask(id, dto);
        return ResponseEntity.ok(updated);
    }

    // --- Admin/HR: delete task ---
    @DeleteMapping("/admin/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // --- Admin/HR: list all tasks ---
    @GetMapping("/admin/tasks")
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // --- Admin/HR: list tasks by employee ---
    @GetMapping("/admin/tasks/employee/{employeeId}")
    public ResponseEntity<List<TaskDTO>> getTasksByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(taskService.getTasksByAssignee(employeeId));
    }

    // --- Employee: get my tasks ---
    @GetMapping("/tasks/my")
    public ResponseEntity<List<TaskDTO>> getMyTasks(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(taskService.getMyTasks(username));
    }

    // --- Employee: update my task status ---
    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<TaskDTO> updateMyTaskStatus(
            @PathVariable Long id,
            @RequestParam(name = "status", required = false) String status,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication
    ) {
        String username = authentication.getName();
        String resolvedStatus = status;
        if ((resolvedStatus == null || resolvedStatus.isBlank()) && body != null && body.get("status") != null) {
            resolvedStatus = String.valueOf(body.get("status"));
        }
        TaskDTO updated = taskService.updateTaskStatusAsEmployee(id, username, resolvedStatus);
        return ResponseEntity.ok(updated);
    }
}
