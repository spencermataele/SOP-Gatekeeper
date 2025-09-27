package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.DeptSubgroup;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.repository.OrgGroupRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository deptRepo;
    private final OrgGroupRepository orgGroupRepo;

    public DepartmentService(DepartmentRepository deptRepo, OrgGroupRepository orgGroupRepo) {
        this.deptRepo = deptRepo;
        this.orgGroupRepo = orgGroupRepo;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> listWithSubgroups() {
        return deptRepo.findAllWithSubgroups()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public DepartmentDto create(@Valid DepartmentDto dto) {
        OrgGroup parent = orgGroupRepo.findById(dto.orgGroupId())
                .orElseThrow(() -> new EntityNotFoundException("OrgGroup %d not found".formatted(dto.orgGroupId())));

        Department d = Department.builder()
                .departmentName(dto.departmentName())
                .orgGroup(parent)
                .build();

        Department saved = deptRepo.save(d);
        return new DepartmentDto(saved.getDepartmentId(), saved.getDepartmentName(),
                parent.getOrgGroupId(), List.of());
    }

    public DepartmentDto update(Integer departmentId, DepartmentCreateDto dto) {
        Department d = deptRepo.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department %d not found".formatted(departmentId)));

        d.setDepartmentName(dto.departmentName());

        if (d.getOrgGroup() == null || !d.getOrgGroup().getOrgGroupId().equals(dto.orgGroupId())) {
            OrgGroup parent = orgGroupRepo.findById(dto.orgGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("OrgGroup %d not found".formatted(dto.orgGroupId())));
            d.setOrgGroup(parent);
        }

        Department saved = deptRepo.save(d);
        Integer parentId = saved.getOrgGroup() != null ? saved.getOrgGroup().getOrgGroupId() : null;

        return new DepartmentDto(saved.getDepartmentId(), saved.getDepartmentName(), parentId, List.of());
    }

    public void delete(Integer departmentId) {
        if (!deptRepo.existsById(departmentId)) {
            throw new EntityNotFoundException("Department %d not found".formatted(departmentId));
        }
        deptRepo.deleteById(departmentId);
    }

    private DepartmentDto toDto(Department d) {
        Integer orgGroupId = d.getOrgGroup() != null ? d.getOrgGroup().getOrgGroupId() : null;
        List<DeptSubgroupSlimDto> subs = (d.getSubgroups() == null) ? List.of()
                : d.getSubgroups().stream().map(this::toSlim).toList();

        return new DepartmentDto(d.getDepartmentId(), d.getDepartmentName(), orgGroupId, subs);
    }

    private DeptSubgroupSlimDto toSlim(DeptSubgroup s) {
        return new DeptSubgroupSlimDto(s.getDeptSubgroupId(), s.getDeptSubgroupName());
    }
}


