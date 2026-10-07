package com.woven.app.service;

import com.woven.app.domain.BusinessProcessFamily;
import com.woven.app.dto.BusinessProcessFamilyCreateDto;
import com.woven.app.dto.BusinessProcessFamilyDto;
import com.woven.app.repository.BusinessProcessFamilyRepository;
import com.woven.app.repository.DeptSubgroupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessProcessFamilyService {
    private final BusinessProcessFamilyRepository families;
    private final DeptSubgroupRepository subgroups;
    public BusinessProcessFamilyDto create(BusinessProcessFamilyCreateDto dto) {
        var family=new BusinessProcessFamily();
        apply(family,dto);
        return toDto(families.save(family));
    }
    public BusinessProcessFamilyDto update(Integer id,BusinessProcessFamilyCreateDto dto) {
        var family=find(id);
        apply(family,dto);
        return toDto(families.save(family));
    }
    private void apply(BusinessProcessFamily family,BusinessProcessFamilyCreateDto dto) {
        var subgroup=subgroups.findById(dto.deptSubgroupId()).orElseThrow(()->new EntityNotFoundException("Subdepartment not found"));
        if(family.getDepartment()!=null && !family.getBusinessProcessList().isEmpty()
                && !family.getDepartment().getDepartmentId().equals(subgroup.getDepartment().getDepartmentId()))
            throw new IllegalArgumentException("Move populated families only within their department. A cross-department move requires a reviewed data migration.");
        family.setBusinessProcessFamilyName(dto.businessProcessFamilyName());
        family.setDeptSubgroup(subgroup);
        family.setDepartment(subgroup.getDepartment());
    }
    private BusinessProcessFamily find(Integer id){return families.findById(id).orElseThrow(()->new EntityNotFoundException("Process family not found"));}
    @Transactional(readOnly=true)
    public BusinessProcessFamilyDto get(Integer id){return toDto(find(id));}
    @Transactional(readOnly=true)
    public List<BusinessProcessFamilyDto> list(){return families.findAll().stream().map(this::toDto).toList();}
    public void delete(Integer id){families.delete(find(id));}
    private BusinessProcessFamilyDto toDto(BusinessProcessFamily family){return new BusinessProcessFamilyDto(
        family.getBusinessProcessFamilyId(),family.getBusinessProcessFamilyName(),
        family.getDepartment().getDepartmentId(),family.getDepartment().getDepartmentName(),
        family.getDeptSubgroup().getDeptSubgroupId(),family.getDeptSubgroup().getDeptSubgroupName(),
        List.of(),family.getCreatedTimestamp(),family.getLastUpdatedTimestamp());}
}
