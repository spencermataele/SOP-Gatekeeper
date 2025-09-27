package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.DeptSubgroup;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.repository.DeptSubgroupRepository;
import com.woven.app.web.dto.admin.DeptSubgroupCreateDto;
import com.woven.app.web.dto.admin.DeptSubgroupDto;
import com.woven.app.web.dto.admin.DeptSubgroupUpdateDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DeptSubgroupService {

    private final DeptSubgroupRepository subgroupRepo;
    private final DepartmentRepository deptRepo;

    public DeptSubgroupService(DeptSubgroupRepository subgroupRepo, DepartmentRepository deptRepo) {
        this.subgroupRepo = subgroupRepo;
        this.deptRepo = deptRepo;
    }

    // Subgroup list
    @Transactional(readOnly = true)
    public List<DeptSubgroupDto> list() {
        return subgroupRepo.findAll().stream().map(this::toDto).toList();
    }

    // Filter subgroup list by department
    @Transactional(readOnly = true)
    public List<DeptSubgroupDto> listByDepartment(Integer departmentId) {
        return subgroupRepo.findByDepartment_DepartmentId(departmentId).stream().map(this::toDto).toList();
    }

    public DeptSubgroupDto create(DeptSubgroupCreateDto dto) {
        Department parent = deptRepo.findById(dto.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department %d not found".formatted(dto.departmentId())));

        DeptSubgroup s = DeptSubgroup.builder()
                .deptSubgroupName(dto.deptSubgroupName())
                .department(parent)
                .build();

        DeptSubgroup saved = subgroupRepo.save(s);
        return new DeptSubgroupDto(saved.getDeptSubgroupId(), saved.getDeptSubgroupName(), parent.getDepartmentId());
    }

    public DeptSubgroupDto update(Integer subgroupId, @Valid DeptSubgroupUpdateDto dto) {
        DeptSubgroup s = subgroupRepo.findById(subgroupId)
                .orElseThrow(() -> new EntityNotFoundException("DeptSubgroup %d not found".formatted(subgroupId)));

        s.setDeptSubgroupName(dto.deptSubgroupName());

        if (s.getDepartment() == null || !s.getDepartment().getDepartmentId().equals(dto.departmentId())) {
            Department parent = deptRepo.findById(dto.departmentId())
                    .orElseThrow(() -> new EntityNotFoundException("Department %d not found".formatted(dto.departmentId())));
            s.setDepartment(parent);
        }

        DeptSubgroup saved = subgroupRepo.save(s);
        Integer parentId = saved.getDepartment() != null ? saved.getDepartment().getDepartmentId() : null;

        return new DeptSubgroupDto(saved.getDeptSubgroupId(), saved.getDeptSubgroupName(), parentId);
    }

    public void delete(Integer subgroupId) {
        if (!subgroupRepo.existsById(subgroupId)) {
            throw new EntityNotFoundException("DeptSubgroup %d not found".formatted(subgroupId));
        }
        subgroupRepo.deleteById(subgroupId);
    }

    private DeptSubgroupDto toDto(DeptSubgroup s) {
        Integer departmentId = s.getDepartment() != null ? s.getDepartment().getDepartmentId() : null;
        return new DeptSubgroupDto(s.getDeptSubgroupId(), s.getDeptSubgroupName(), departmentId);
    }
}

