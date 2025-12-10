package com.example.hrms.mapper;

import com.example.hrms.dto.TaskDTO;
import com.example.hrms.entity.Task;

public class TaskMapper {

    public static TaskDTO toDTO(Task entity) {
        if (entity == null) return null;
        return TaskDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .dueDate(entity.getDueDate())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .assigneeId(entity.getAssigneeId())
                .assigneeName(entity.getAssigneeName())
                .createdById(entity.getCreatedById())
                .createdByName(entity.getCreatedByName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static Task toEntity(TaskDTO dto) {
        if (dto == null) return null;
        return Task.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .priority(dto.getPriority())
                .status(dto.getStatus())
                .assigneeId(dto.getAssigneeId())
                .assigneeName(dto.getAssigneeName())
                .createdById(dto.getCreatedById())
                .createdByName(dto.getCreatedByName())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
