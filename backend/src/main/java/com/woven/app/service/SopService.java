package com.woven.app.service;

import com.woven.app.domain.Sop;
import com.woven.app.dto.SopDto;
import com.woven.app.repository.SopRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SopService {
    private final SopRepository sopRepository;

    public SopService(SopRepository sopRepository) {
        this.sopRepository = sopRepository;
    }

    @Transactional(readOnly = true)
    public List<SopDto> list() {
        return sopRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SopDto get(Integer id) {
        return toDto(findOrThrow(id));
    }

    public SopDto create(SopDto dto) {
        Sop entity = new Sop();
        apply(dto, entity);
        Sop saved = sopRepository.save(entity);
        return toDto(saved);
    }

    public SopDto update(Integer id, SopDto dto) {
        Sop entity = findOrThrow(id);
        entity.setSop_id(id);
        apply(dto, entity);
        Sop saved = sopRepository.save(entity);
        return toDto(saved);
    }

    public void delete(Integer id) {
        if (!sopRepository.existsById(id)) {
            throw new EntityNotFoundException("Sop not found" + id);
        }
        sopRepository.deleteById(id);
    }

    private SopDto toDto(Sop sop) {
        return new SopDto(
                sop.getSop_id(),
                sop.getTitle(),
                sop.getAuthor_id(),
                sop.getOrg_id(),
                sop.getOrg_group_id(),
                sop.getDepartment_id(),
                sop.getDept_subgroup_id(),
                sop.getCurrent_process_owner_id(),
                sop.getCurrent_process_owner_position_id(),
                sop.getProcess_id(),
                sop.getProcess_name(),
                sop.getProcess_family_id(),
                sop.getParent_process_id(),
                sop.getSop_location_path(),
                sop.getCreated_timestamp(),
                sop.getUpdated_timestamp(),
                sop.getVersion_id(),
                sop.getSopDetails()
        );
    }

    private Sop findOrThrow(Integer id) {
        return sopRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    private void apply(SopDto dto, Sop entity) {
        if (dto.sopId() !=null) {
            entity.setSop_id(dto.sopId());
        }

        entity.setTitle(dto.title());
        entity.setAuthor_id(dto.authorId());
        entity.setOrg_id(dto.orgId());
        entity.setOrg_group_id(dto.orgGroupId());
        entity.setDepartment_id(dto.departmentId());
        entity.setDept_subgroup_id(dto.deptSubgroupId());
        entity.setCurrent_process_owner_id(dto.currentProcessOwnerId());
        entity.setCurrent_process_owner_position_id(dto.currentProcessOwnerPositionId());
        entity.setProcess_id(dto.processId());
        entity.setProcess_name(dto.processName());
        entity.setProcess_family_id(dto.processFamilyId());
        entity.setParent_process_id(dto.parentProcessId());
        entity.setSop_location_path(dto.sopLocationPath());
        // created_timestamp created by db trigger
        // updated_timestamp created by db trigger
        entity.setVersion_id(dto.versionId());
        entity.setSopDetails(dto.sopDetails());
    }
}
