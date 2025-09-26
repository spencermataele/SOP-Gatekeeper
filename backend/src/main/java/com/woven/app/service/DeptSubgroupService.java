package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.DeptSubgroup;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.repository.DeptSubgroupRepository;
import jakarta.persistence.EntityNotFoundException;
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

    @Transactional(readOnly = true)
    public List<DeptSubgroup> list() {
        return subgroupRepo.findAll();
    }

    @Transactional(readOnly = true)
    public DeptSubgroup get(Integer id) {
        return subgroupRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DeptSubgroup " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<DeptSubgroup> listByDepartment(Integer departmentId) {
        return subgroupRepo.findByDepartment_DepartmentId(departmentId);
    }

    public DeptSubgroup create(Integer departmentId, String deptSubgroupName) {
        Department dept = deptRepo.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department " + departmentId + " not found"));

        DeptSubgroup sg = DeptSubgroup.builder()
                .deptSubgroupName(deptSubgroupName)
                .department(dept)
                .build();

        return subgroupRepo.save(sg);
    }

    public DeptSubgroup update(Integer id, Integer departmentId, String deptSubgroupName) {
        DeptSubgroup existing = get(id);

        if (departmentId != null && (existing.getDepartment() == null
                || !departmentId.equals(existing.getDepartment().getDepartmentId()))) {
            Department newDept = deptRepo.findById(departmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Department " + departmentId + " not found"));
            existing.setDepartment(newDept);
        }

        if (deptSubgroupName != null && !deptSubgroupName.isBlank()) {
            existing.setDeptSubgroupName(deptSubgroupName);
        }

        return subgroupRepo.save(existing);
    }

    public void delete(Integer id) {
        subgroupRepo.deleteById(id);
    }
}

