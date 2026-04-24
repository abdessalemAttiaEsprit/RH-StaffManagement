package com.smartpark.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "personnel_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelRequest {
    @Id
    private String id;

    private String matricule;
    private String fullPersonnelName;

    private String message;
    private LocalDateTime createdAt;
    private String status;
}

