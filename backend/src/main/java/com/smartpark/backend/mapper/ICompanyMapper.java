package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import com.smartpark.backend.dto.CompanyDTO;
import com.smartpark.backend.model.Absence;
import com.smartpark.backend.model.CompanySettings;


public interface ICompanyMapper {

    @Mapper(componentModel = "spring")
    public interface IAbsenceMapper {
        CompanyDTO toDto(CompanySettings cs);

        CompanySettings toEntity(CompanyDTO absenceDTO);
    }

}

