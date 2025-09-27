package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.DeptSubgroup;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.web.dto.admin.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {
    private final DepartmentRepository repo;

    public DepartmentService(DepartmentRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<DepartmentDto> listWithSubgroups() {
        List<Department> depts = repo.findAllWithSubgroups();
        return depts.stream().map(this::toDto).toList();
    }

    private DepartmentDto toDto(Department d) {
        List<DeptSubgroupSlimDto> subs = d.getSubgroups() == null ? List.of()
                : d.getSubgroups().stream()
                .map(this::toSlim)
                .toList();

        Integer orgGroupId = (d.getOrgGroup() != null) ? d.getOrgGroup().getOrgGroupId() : null;
        return new DepartmentDto(d.getDepartmentId(), d.getDepartmentName(), orgGroupId, subs);
    }

    private DeptSubgroupSlimDto toSlim(DeptSubgroup s) {
        return new DeptSubgroupSlimDto(s.getDeptSubgroupId(), s.getDeptSubgroupName());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDto(Integer id) {
        Department d = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Department " + id + " not found"));
        Integer orgGroupId = (d.getOrgGroup() != null) ? d.getOrgGroup().getOrgGroupId() : null;
        return new DepartmentDto(d.getDepartmentId(), d.getDepartmentName(), orgGroupId, List.of());
    }
}


