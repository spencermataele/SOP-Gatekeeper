package com.woven.app.service;

import com.woven.app.domain.ProcessOwner;
import com.woven.app.dto.ProcessOwnerCreateDto;
import com.woven.app.repository.ProcessOwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProcessOwnerService {

    private final ProcessOwnerRepository repo;

    public ProcessOwnerService(ProcessOwnerRepository repo) {
        this.repo = repo;
    }

    public List<ProcessOwner> list() {
        return repo.findAll();
    }

    public ProcessOwner get(Integer id) {
        return repo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Process owner not found: " + id));
    }

    public ProcessOwner create(@Valid ProcessOwnerCreateDto dto) {
        ProcessOwner po = new ProcessOwner();
        applyDto(po, dto);
        return repo.save(po); // ✅ save the ENTITY, not the DTO
    }

    public ProcessOwner update(Integer id, @Valid ProcessOwnerCreateDto dto) {
        ProcessOwner po = get(id);
        applyDto(po, dto);
        return repo.save(po); // ✅ save the ENTITY
    }

    public void delete(Integer id) {
        repo.deleteById(id);
    }

    // --- helper to map DTO -> entity
    private void applyDto(ProcessOwner po, ProcessOwnerCreateDto dto) {
        // If your record fields are processOwnerName/processOwnerPositionId:
        po.setName(dto.name());
        po.setPositionId(dto.positionId());
    }
}

