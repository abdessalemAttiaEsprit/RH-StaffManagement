package com.smartpark.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "settings")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompanySettings {
    @Id
    private String id;
    private String companyName;
    private String address;
    private String matriculeFiscal;
    private String logoBase64; // Pour stocker l'image en texte
    private String signatureFileName;
    private String phone;
}
