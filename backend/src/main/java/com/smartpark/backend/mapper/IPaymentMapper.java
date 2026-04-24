package com.smartpark.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.model.Payment;
import com.smartpark.backend.model.Personnel;

@Mapper(componentModel = "spring")
public interface IPaymentMapper {

    @Mapping(source = "month", target = "month")
    PaymentDTO toDto(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentDate", expression = "java(java.time.LocalDate.now())")
    Payment toEntity(PaymentDTO paymentDto);
}
