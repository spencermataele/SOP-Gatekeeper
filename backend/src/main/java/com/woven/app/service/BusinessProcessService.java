package com.woven.app.service;
import com.woven.app.domain.*;
import com.woven.app.dto.*;
import com.woven.app.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional
public class BusinessProcessService {
 private final BusinessProcessRepository processes;
 private final BusinessProcessFamilyRepository families;
 public BusinessProcessDto create(BusinessProcessCreateDto dto){var process=new BusinessProcess();apply(process,dto);return toDto(processes.save(process));}
 public BusinessProcessDto update(Integer id,BusinessProcessCreateDto dto){var process=find(id);apply(process,dto);return toDto(processes.save(process));}
 private BusinessProcess find(Integer id){return processes.findById(id).orElseThrow(()->new EntityNotFoundException("Process not found"));}
 private void apply(BusinessProcess process,BusinessProcessCreateDto dto){
  var family=families.findById(dto.businessProcessFamilyId()).orElseThrow(()->new EntityNotFoundException("Process family not found"));
  if(dto.departmentId()!=null&&!dto.departmentId().equals(family.getDepartment().getDepartmentId())) throw new IllegalArgumentException("Department must match the process family");
  if(dto.deptSubGroupIds()!=null && dto.deptSubGroupIds().stream().anyMatch(id->!id.equals(family.getDeptSubgroup().getDeptSubgroupId()))) throw new IllegalArgumentException("Subdepartment is inherited from the process family");
  if(process.getBusinessProcessFamily()!=null&&!process.getBusinessProcessFamily().getDepartment().getDepartmentId().equals(family.getDepartment().getDepartmentId())) throw new IllegalArgumentException("Cross-department process moves require a reviewed data migration");
  BusinessProcess parent=null;
  if(dto.parentBusinessProcessId()!=null){parent=find(dto.parentBusinessProcessId());if(!parent.getBusinessProcessFamily().getBusinessProcessFamilyId().equals(family.getBusinessProcessFamilyId()))throw new IllegalArgumentException("Parent process must belong to the same family");
   var ancestor=parent;Set<Integer> seen=new HashSet<>();while(ancestor!=null){if(Objects.equals(ancestor.getBusinessProcessId(),process.getBusinessProcessId())||!seen.add(ancestor.getBusinessProcessId()))throw new IllegalArgumentException("Process relationships cannot contain a cycle");ancestor=ancestor.getParentBusinessProcess();}}
  process.setBusinessProcessName(dto.businessProcessName());process.setBusinessProcessFamily(family);process.setParentBusinessProcess(parent);
  process.getDeptSubgroups().clear();process.getDeptSubgroups().add(family.getDeptSubgroup());
 }
 @Transactional(readOnly=true) public BusinessProcessDto get(Integer id){return toDto(find(id));}
 @Transactional(readOnly=true) public List<BusinessProcessDto> list(){return processes.findAll().stream().map(this::toDto).toList();}
 public void delete(Integer id){processes.delete(find(id));}
 private BusinessProcessDto toDto(BusinessProcess p){var f=p.getBusinessProcessFamily();var parent=p.getParentBusinessProcess();var s=f.getDeptSubgroup();return new BusinessProcessDto(p.getBusinessProcessId(),p.getBusinessProcessName(),f.getBusinessProcessFamilyId(),f.getBusinessProcessFamilyName(),parent==null?null:parent.getBusinessProcessId(),parent==null?null:parent.getBusinessProcessName(),f.getDepartment().getDepartmentId(),f.getDepartment().getDepartmentName(),List.of(s.getDeptSubgroupId()),List.of(s.getDeptSubgroupName()),p.getLastUpdatedTimestamp());}
}
