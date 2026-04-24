package com.smartpark.backend.model;

import lombok.*;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import com.smartpark.backend.dto.AbsenceDTO;

@Document(collection = "personnels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Personnel {
    @Id
    private String id;
    private String nom;
    private String prenom;
    private String telephone;
    @Indexed(unique = true)

    private String email;
    @Indexed(unique = true)

    private String cin;
    @Indexed(unique = true)

    private String matricule;
    @Indexed(unique = true)

    private String cnssNumber ;
    @Indexed(unique = true)
    private String rib;
    private List<Absence> absences = new ArrayList<>() ;
    private Contract contrat;
}

