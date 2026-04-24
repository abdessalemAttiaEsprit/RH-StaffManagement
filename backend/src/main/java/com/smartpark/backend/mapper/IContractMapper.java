package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.model.Contract;

@Mapper(componentModel = "spring")
public interface IContractMapper  {

    // Convertit l'entité en DTO
    ContractDTO toDto(Contract contract);

    // Convertit le DTO en entité
    Contract toEntity(ContractDTO contractDTO);
}
