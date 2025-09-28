package com.woven.app.service;


import com.woven.app.domain.BusinessProcessFamily;
import com.woven.app.dto.BusinessProcessFamilyCreateDto;
import com.woven.app.dto.BusinessProcessFamilyDto;
import com.woven.app.repository.BusinessProcessFamilyRepository;
import com.woven.app.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessProcessFamilyService {
    private final BusinessProcessFamilyRepository businessProcessFamilyRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public BusinessProcessFamilyDto create(BusinessProcessFamilyCreateDto businessProcessFamilyCreateDto) {
        var dept = departmentRepository.findById(
                businessProcessFamilyCreateDto.departmentId()
                ).orElseThrow();
        var bpf = new BusinessProcessFamily();
        bpf.setBusinessProcessFamilyName(businessProcessFamilyCreateDto.businessProcessFamilyName());
        bpf.setDepartment(dept);
        var saved = businessProcessFamilyRepository.save(bpf);
        return new BusinessProcessFamilyDto(saved.getBusinessProcessFamilyId(), saved.getBusinessProcessFamilyName(),
                dept.getDepartmentId(), dept.getDepartmentName());
    }

    @Transactional(readOnly = true)
    public List<BusinessProcessFamilyDto> list(){
        return businessProcessFamilyRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList(); //Uses mapper
    }

    @Transactional(readOnly = true)
    public BusinessProcessFamilyDto get(Integer id) {
        var bpf = businessProcessFamilyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BusinessProcessFamily not found: " + id));
        return toDto(bpf);
    }

    @Transactional
    public BusinessProcessFamilyDto update(Integer id, @Valid BusinessProcessFamilyCreateDto dto) {
        var bpf = businessProcessFamilyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BusinessProcessFamily not found: " + id));

        // Update fields if provided
        if (dto.businessProcessFamilyName() != null && !dto.businessProcessFamilyName().isBlank()) {
            bpf.setBusinessProcessFamilyName(dto.businessProcessFamilyName());
        }
        if (dto.departmentId() != null) {
            var dept = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found: " + dto.departmentId()));
            bpf.setDepartment(dept);
        }

        var saved = businessProcessFamilyRepository.save(bpf);
        return toDto(saved);
    }

    @Transactional
    public void delete(Integer id) {
        if (!businessProcessFamilyRepository.existsById(id)) {
            throw new EntityNotFoundException("BusinessProcessFamily not found: " + id);
        }
        businessProcessFamilyRepository.deleteById(id);
    }

    // Mapper
    private BusinessProcessFamilyDto toDto(BusinessProcessFamily bpf) {
        return new BusinessProcessFamilyDto(
                bpf.getBusinessProcessFamilyId().intValue(),
                bpf.getBusinessProcessFamilyName(),
                bpf.getDepartment() != null ? bpf.getDepartment().getDepartmentId().intValue() : null,
                bpf.getDepartment() != null ? bpf.getDepartment().getDepartmentName() : null
        );
    }

}
