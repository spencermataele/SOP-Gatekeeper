package com.woven.app.service;

import com.woven.app.domain.ProcessOwner;
import com.woven.app.repository.ProcessOwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;


public class ProcessOwnerService {
    private final ProcessOwnerRepository processOwnerRepository;
    public ProcessOwnerService(ProcessOwnerRepository processOwnerRepository) {
        this.processOwnerRepository = processOwnerRepository;
    }

    public List<ProcessOwner> list() {
        return processOwnerRepository.findAll();
    }

    public ProcessOwner get(Integer id) {
        return processOwnerRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Process owner not found: " + id)
        );
    }

    public ProcessOwner create(ProcessOwner po) { return processOwnerRepository.save(po); }

    public ProcessOwner update(Integer id, ProcessOwner po) {
        ProcessOwner existing = get(id);
        existing.setProcessOwnerName(po.getProcessOwnerName());
        existing.setProcessOwnerPositionId(po.getProcessOwnerPositionId());
        existing.setParent(po.getParent());
        return processOwnerRepository.save(existing);
    }

    public void delete(Integer id) { processOwnerRepository.deleteById(id); }
}
