package com.woven.app.service;

import com.woven.app.domain.Sop;
import com.woven.app.domain.SopStatus;
import com.woven.app.dto.SopDto;
import com.woven.app.repository.SopRepository;
import com.woven.app.service.user.AppUserDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        //show only active sops
        return sopRepository.findByStatus(SopStatus.ACTIVE).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SopDto get(Integer id) {
        Sop sop = sopRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Sop not found")
        );
        return toDto(sop);
    }

    public SopDto create(SopDto dto, AppUserDetails currentUser) {
        Sop entity = new Sop();
        apply(dto, entity);
        //Set authorId as current user's
        entity.setAuthorId(currentUser.getUser().getId());
        //Set status to "ACTIVE"
        entity.setStatus(SopStatus.ACTIVE);
        entity.setIsActive(true);
        entity.setPublishedTimestamp(Instant.now());
        Sop saved = sopRepository.save(entity);
        return toDto(saved);
    }

    // Updates need to only be allowed within change management workflow
    public SopDto update(Integer id, SopDto dto) {
        Sop entity = findOrThrow(id);

        if (entity.getStatus() != SopStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT SOPs may be edited");
        }

        apply(dto, entity);
        Sop saved = sopRepository.save(entity);
        return toDto(saved);
    }

    /*  Delete not necessary with version control
    public void delete(Integer id) {
        if (!sopRepository.existsById(id)) {
            throw new EntityNotFoundException("Sop not found" + id);
        }
        sopRepository.deleteById(id);
    }
    */

    private SopDto toDto(Sop sop) {
        return new SopDto(
                sop.getSopId(),
                sop.getTitle(),
                sop.getAuthorId(),
                sop.getOrgId(),
                sop.getOrgGroupId(),
                sop.getDepartmentId(),
                sop.getDeptSubgroupId(),
                sop.getCurrentProcessOwnerId(),
                sop.getCurrentProcessOwnerPositionId(),
                sop.getProcessId(),
                sop.getProcessName(),
                sop.getProcessFamilyId(),
                sop.getParentProcessId(),
                sop.getCreatedTimestamp(),
                sop.getUpdatedTimestamp(),
                sop.getVersionId(),
                sop.getSopDescription(),
                sop.getSopDetails()
        );
    }

    private Sop findOrThrow(Integer id) {
        return sopRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    private void apply(SopDto dto, Sop entity) {
        if (dto.sopId() !=null) {
            entity.setSopId(dto.sopId());
        }

        entity.setTitle(dto.title());
        entity.setAuthorId(dto.authorId());
        entity.setOrgId(dto.orgId());
        entity.setOrgGroupId(dto.orgGroupId());
        entity.setDepartmentId(dto.departmentId());
        entity.setDeptSubgroupId(dto.deptSubgroupId());
        entity.setCurrentProcessOwnerId(dto.currentProcessOwnerId());
        entity.setCurrentProcessOwnerPositionId(dto.currentProcessOwnerPositionId());
        entity.setProcessId(dto.processId());
        entity.setProcessName(dto.processName());
        entity.setProcessFamilyId(dto.processFamilyId());
        entity.setParentProcessId(dto.parentProcessId());
        // created_timestamp created by db trigger
        // updated_timestamp created by db trigger
        // version updated when creating draft
        entity.setSopDescription(dto.sopDescription());
        entity.setSopDetails(dto.sopDetails());
    }
}
