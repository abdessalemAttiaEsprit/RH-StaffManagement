package com.smartpark.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalaryAiRequest {
    private Integer roleId;
    private Integer experience;
    private Double cvScore;
    private Integer contractTypeId;
}

