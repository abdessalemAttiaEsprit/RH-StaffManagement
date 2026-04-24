package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import com.smartpark.backend.dto.InterviewDTO;
import com.smartpark.backend.model.Interview;

@Mapper(componentModel = "spring")
public interface InterviewMapper {
    InterviewDTO toDTO(Interview interview);
    Interview toEntity(InterviewDTO dto);
}

