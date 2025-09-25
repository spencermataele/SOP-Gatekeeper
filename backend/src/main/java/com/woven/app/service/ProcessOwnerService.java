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
    private final ProcessOwnerRepository processOwnerRepository;

    public ProcessOwnerService(ProcessOwnerRepository processOwnerRepository) {
        this.processOwnerRepository = processOwnerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProcessOwner> list() {
        return processOwnerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProcessOwner get(Integer id) {
        return processOwnerRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Process owner not found: " + id)
        );
    }

    public ProcessOwner create(@Valid ProcessOwnerCreateDto po) { return processOwnerRepository.save(po); }

    public ProcessOwner update(Integer id, @Valid ProcessOwnerCreateDto po) {
        ProcessOwner existing = get(id);
        existing.setName(po.getName());
        existing.setPositionId(po.getPositionId());
        existing.setParent(po.getParent());
        return processOwnerRepository.save(existing);
    }

    public void delete(Integer id) { processOwnerRepository.deleteById(id); }
}
