package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import com.smartpark.backend.dto.AbsenceDTO;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.model.Absence;
import com.smartpark.backend.model.Contract;

@Mapper(componentModel = "spring")
public interface IAbsenceMapper {
    // Convertit l'entité en DTO
    AbsenceDTO toDto(Absence absence);

    // Convertit le DTO en entité
     Absence toEntity(AbsenceDTO absenceDTO);
}

