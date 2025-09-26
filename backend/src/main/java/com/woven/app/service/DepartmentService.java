package com.woven.app.service;

import com.woven.app.domain.Department;
import com.woven.app.domain.OrgGroup;
import com.woven.app.repository.DepartmentRepository;
import com.woven.app.repository.OrgGroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository deptRepo;
    private final OrgGroupRepository groupRepo;

    public DepartmentService(DepartmentRepository deptRepo, OrgGroupRepository groupRepo) {
        this.deptRepo = deptRepo;
        this.groupRepo = groupRepo;
    }

    @Transactional(readOnly = true)
    public List<Department> list() {
        return deptRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Department get(Integer id) {
        return deptRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Department> listByOrgGroup(Integer orgGroupId) {
        return deptRepo.findByOrgGroup_OrgGroupId(orgGroupId);
    }

    public Department create(Integer orgGroupId, String departmentName) {
        OrgGroup group = groupRepo.findById(orgGroupId)
                .orElseThrow(() -> new EntityNotFoundException("OrgGroup " + orgGroupId + " not found"));

        Department d = Department.builder()
                .departmentName(departmentName)
                .orgGroup(group)
                .build();

        return deptRepo.save(d);
    }

    public Department update(Integer id, Integer orgGroupId, String departmentName) {
        Department existing = get(id);

        if (orgGroupId != null && (existing.getOrgGroup() == null
                || !orgGroupId.equals(existing.getOrgGroup().getOrgGroupId()))) {
            OrgGroup newGroup = groupRepo.findById(orgGroupId)
                    .orElseThrow(() -> new EntityNotFoundException("OrgGroup " + orgGroupId + " not found"));
            existing.setOrgGroup(newGroup);
        }

        if (departmentName != null && !departmentName.isBlank()) {
            existing.setDepartmentName(departmentName);
        }

        return deptRepo.save(existing);
    }

    public void delete(Integer id) {
        // prevent deleting if subgroups exist
        if (!get(id).getSubgroups().isEmpty()) {
            throw new IllegalStateException("Department has subgroups; delete or move them first.");
        }
        deptRepo.deleteById(id);
    }
}
