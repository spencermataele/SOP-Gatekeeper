package com.woven.app.service;

import com.woven.app.domain.Org;
import com.woven.app.repository.OrgRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrgService {

    private final OrgRepository orgRepository;
    public OrgService(OrgRepository orgRepository) { this.orgRepository = orgRepository; }

    @Transactional(readOnly = true) public List<Org> list() {
        return orgRepository.findAll();
    }

    @Transactional(readOnly = true) public Org get(Integer id) {
        return orgRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Org "+id+" not found"));
    }
    public Org create(Org org) { return orgRepository.save(org); }
    public Org update(Integer id, Org body) {
        Org o = get(id);
        o.setOrgName(body.getOrgName());
        return orgRepository.save(o);
    }
    public void delete(Integer id) { orgRepository.deleteById(id); }
}
