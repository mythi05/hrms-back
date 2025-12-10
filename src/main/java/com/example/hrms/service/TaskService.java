package com.example.hrms.service;

import com.example.hrms.dto.TaskDTO;

import java.util.List;

public interface TaskService {

    TaskDTO createTask(TaskDTO dto, String creatorUsername);

    TaskDTO updateTask(Long id, TaskDTO dto);

    void deleteTask(Long id);

    List<TaskDTO> getAllTasks();

    List<TaskDTO> getTasksByAssignee(Long employeeId);

    List<TaskDTO> getMyTasks(String username);

    TaskDTO updateTaskStatusAsEmployee(Long taskId, String username, String status);
}
