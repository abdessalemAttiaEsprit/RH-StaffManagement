package com.smartpark.backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompanyDTO {
    private String companyName;
    private String address;
    private String matriculeFiscal;
    private String logoBase64; // Pour stocker l'image en texte
    private String phone;
}

