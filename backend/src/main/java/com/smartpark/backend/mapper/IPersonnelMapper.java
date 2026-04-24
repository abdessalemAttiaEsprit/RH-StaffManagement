package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.model.Personnel;

@Mapper(componentModel = "spring", uses = {IContractMapper.class})
public interface IPersonnelMapper {

    PersonnelDTO toDto(Personnel personnel);

    @Mapping(target = "id", ignore = true)
    Personnel toEntity(PersonnelDTO personnelDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "matricule", ignore = true)
    void updatePersonnelFromDto(PersonnelDTO dto, @MappingTarget Personnel entity);
}
