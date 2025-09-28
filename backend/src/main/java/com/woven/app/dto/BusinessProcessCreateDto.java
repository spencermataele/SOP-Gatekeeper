package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BusinessProcessCreateDto (
        @NotBlank @Size(max = 255)
        String businessProcessName,
        @NotNull
        Integer businessProcessFamilyId,
        Integer parentBusinessProcessId,
        @NotNull
        Integer departmentId,
        List<Integer> deptSubGroupIds
){
}
