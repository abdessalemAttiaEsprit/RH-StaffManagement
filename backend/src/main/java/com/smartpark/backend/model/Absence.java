package com.smartpark.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
@Document(collection = "absences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Absence {
    @Id
    private String id;
    private LocalDate startDate;
    private LocalDate endDate ;
    private String status ; //Absent or Present
    private String typeAbsence ;
    private String justification;//Url
}

