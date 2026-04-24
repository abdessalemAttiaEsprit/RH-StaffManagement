package com.smartpark.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelRequestDTO {
    private String id;
    private String matricule;
    private String fullPersonnelName;
    private String message;
    private LocalDateTime createdAt;
    private String status;
}

