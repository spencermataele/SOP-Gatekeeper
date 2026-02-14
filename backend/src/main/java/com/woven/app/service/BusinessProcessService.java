package com.woven.app.service;

import com.woven.app.domain.BusinessProcess;
import com.woven.app.domain.DeptSubgroup;
import com.woven.app.dto.BusinessProcessCreateDto;
import com.woven.app.dto.BusinessProcessDto;
import com.woven.app.repository.BusinessProcessFamilyRepository;
import com.woven.app.repository.BusinessProcessRepository;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.repository.DeptSubgroupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessProcessService {
    private final BusinessProcessRepository businessProcessRepository;
    private final BusinessProcessFamilyRepository businessProcessFamilyRepository;
    private final DepartmentRepository departmentRepository;
    private final DeptSubgroupRepository deptSubgroupRepository;

    // Create
    @Transactional
    public BusinessProcessDto create(BusinessProcessCreateDto businessProcessCreateDto) {
        var fam = businessProcessFamilyRepository.findById(
                        businessProcessCreateDto.businessProcessFamilyId().intValue())
                .orElseThrow(() -> new EntityNotFoundException(" Business Process Family not found"));

        // If parent
        BusinessProcess parentBusinessProcess = null;
        if (businessProcessCreateDto.parentBusinessProcessId() != null) {
            parentBusinessProcess = businessProcessRepository.findById(businessProcessCreateDto.parentBusinessProcessId()
                    .intValue()).orElseThrow(() -> new EntityNotFoundException("Parent Business Process not found"));
        }

        // Dept validation
        if (businessProcessCreateDto.departmentId() != null) {
            var dept = departmentRepository.findById(businessProcessCreateDto.departmentId().intValue())
                    .orElseThrow(() -> new EntityNotFoundException("Department not found" + businessProcessCreateDto.departmentId()));
            if (fam.getDepartment() == null || !fam.getDepartment().getDepartmentId().equals(dept.getDepartmentId())) {
            }
        }

        // Build it
        var bp = new BusinessProcess();
        bp.setBusinessProcessName(businessProcessCreateDto.businessProcessName());
        bp.setBusinessProcessFamily(fam);
        bp.setParentBusinessProcess(parentBusinessProcess);

        // Subgroups
        if (businessProcessCreateDto.deptSubGroupIds() != null && !businessProcessCreateDto.deptSubGroupIds().isEmpty()) {
            var subgroups = new HashSet<DeptSubgroup>(
                    deptSubgroupRepository.findAllById(
                            businessProcessCreateDto.deptSubGroupIds().stream().map(Integer::valueOf).toList()
                    )
            );
            if (subgroups.size() != businessProcessCreateDto.deptSubGroupIds().size()) {
                throw new EntityNotFoundException(" One or more Dept Subgroups not found");
            }
            bp.setDeptSubgroups(subgroups);
        }

        var saved = businessProcessRepository.save(bp);
        return toDto(saved);
    }

    // Read
    @Transactional(readOnly = true)
    public BusinessProcessDto get(Integer businessProcessId) {
        var bp = businessProcessRepository.findById(businessProcessId)
                .orElseThrow(() -> new EntityNotFoundException("Business Process not found" + businessProcessId));
        return toDto(bp);
    }

    @Transactional(readOnly = true)
    public List<BusinessProcessDto> list() {
        return businessProcessRepository.findForProcessStreamReport(
                null,
                null,
                null,
                null,
                null
        ).stream().map(
                this::toDto
        ).toList();
    }

    // Update
    @Transactional
    public BusinessProcessDto update(Integer businessProcessId, BusinessProcessCreateDto businessProcessCreateDto) {
        var bp = businessProcessRepository.findById(businessProcessId)
                .orElseThrow(() -> new EntityNotFoundException("Business Process not found" + businessProcessId));

        if (businessProcessCreateDto.businessProcessName() != null && !businessProcessCreateDto.businessProcessName().isBlank()) {
            bp.setBusinessProcessName(businessProcessCreateDto.businessProcessName());
        }

        if (businessProcessCreateDto.businessProcessFamilyId() != null) {
            var family = businessProcessFamilyRepository.findById(businessProcessCreateDto.businessProcessFamilyId().intValue())
                    .orElseThrow(() -> new EntityNotFoundException("BusinessProcessFamily not found: " + businessProcessCreateDto.businessProcessFamilyId()));
            bp.setBusinessProcessFamily(family);
        }

        if (businessProcessCreateDto.parentBusinessProcessId() != null) {
            var parentBusinessProcess = businessProcessRepository.findById(businessProcessCreateDto.parentBusinessProcessId().intValue())
                    .orElseThrow(() -> new EntityNotFoundException("Parent BusinessProcess not found: " + businessProcessCreateDto.parentBusinessProcessId()));
            bp.setParentBusinessProcess(parentBusinessProcess);
        }

        if (businessProcessCreateDto.deptSubGroupIds() != null) {
            var subgroups = new HashSet<DeptSubgroup>(
                    deptSubgroupRepository.findAllById(
                            businessProcessCreateDto.deptSubGroupIds().stream().map(Integer::valueOf).toList()
                    )
            );
            if (subgroups.size() != businessProcessCreateDto.deptSubGroupIds().size()) {
                throw new EntityNotFoundException("One or more DeptSubgroup IDs were not found.");
            }
            bp.getDeptSubgroups().clear();
            bp.getDeptSubgroups().addAll(subgroups);
        }

        var saved = businessProcessRepository.save(bp);
        return toDto(saved);
    }

    // Delete
    @Transactional
    public void delete(Integer businessProcessId) {
        var exists = businessProcessRepository.existsById(businessProcessId);
        if (!exists) {
            throw new EntityNotFoundException("Business Process not found" + businessProcessId);
        }
        businessProcessRepository.deleteById(businessProcessId);
    }

    private BusinessProcessDto toDto(BusinessProcess businessProcess) {
        var family = businessProcess.getBusinessProcessFamily();
        var parentProcess = businessProcess.getParentBusinessProcess();

        var deptSubgroupIds = businessProcess.getDeptSubgroups().stream()
                .map(DeptSubgroup::getDeptSubgroupId)
                .map(Integer::intValue).toList();

        var deptSubgroupNames = businessProcess.getDeptSubgroups().stream()
                .map(DeptSubgroup::getDeptSubgroupName)
                .toList();

        Integer departmentId = null;
        String departmentName = null;

        if (family != null && family.getDepartment().getDepartmentName() != null) {
            departmentId = family.getDepartment().getDepartmentId().intValue();
            departmentName = family.getDepartment().getDepartmentName();
        }

        return new BusinessProcessDto(
                businessProcess.getBusinessProcessId() != null ? businessProcess.getBusinessProcessId().intValue(): null,
                businessProcess.getBusinessProcessName(),
                family != null ? family.getBusinessProcessFamilyId() : null,
                family != null ? family.getBusinessProcessFamilyName() : null,
                parentProcess != null ? parentProcess.getBusinessProcessId().intValue() : null,
                parentProcess != null ? parentProcess.getBusinessProcessName() : null,
                departmentId,
                departmentName,
                deptSubgroupIds,
                deptSubgroupNames,
                businessProcess.getLastUpdatedTimestamp()
        );
    }
}
