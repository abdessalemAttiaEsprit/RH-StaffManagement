package com.smartpark.backend.dto;
import java.time.LocalDate;
import java.util.Map;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AbsenceDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private String status ; //Absent or Present
    private String typeAbsence ;
    private String justification;//Url
}
