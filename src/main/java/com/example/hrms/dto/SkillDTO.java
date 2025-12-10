package com.example.hrms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDTO {
    private String name;
    private String level; // String để frontend dễ bind
}
