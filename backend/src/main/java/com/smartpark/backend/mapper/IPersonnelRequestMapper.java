package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import com.smartpark.backend.dto.PersonnelRequestDTO;
import com.smartpark.backend.model.PersonnelRequest;

@Mapper(componentModel = "spring")
public interface IPersonnelRequestMapper {
    PersonnelRequestDTO toDto(PersonnelRequest entity);
    PersonnelRequest toEntity(PersonnelRequestDTO dto);
}

