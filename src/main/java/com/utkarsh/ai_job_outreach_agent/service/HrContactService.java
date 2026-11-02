package com.utkarsh.ai_job_outreach_agent.service;

import com.utkarsh.ai_job_outreach_agent.dto.HrContactRequest;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.repository.HrContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HrContactService {
    private final HrContactRepository repository;
    public HrContactService(HrContactRepository repository) { this.repository = repository; }

    public List<HrContact> findAll() { return repository.findAll(); }

    @Transactional
    public HrContact create(HrContactRequest request) {
        String email = request.email().trim().toLowerCase();
        if (repository.findByEmailIgnoreCase(email).isPresent())
            throw new IllegalArgumentException("A contact with this email already exists");
        HrContact c = new HrContact();
        apply(c, request);
        return repository.save(c);
    }

    @Transactional
    public HrContact update(Long id, HrContactRequest request) {
        HrContact c = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("HR contact not found: " + id));
        repository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalArgumentException("Email already belongs to another contact"); });
        apply(c, request);
        return repository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("HR contact not found: " + id);
        repository.deleteById(id);
    }

    public HrContact get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("HR contact not found: " + id));
    }

    private void apply(HrContact c, HrContactRequest r) {
        c.setName(r.name().trim());
        c.setEmail(r.email().trim().toLowerCase());
        c.setCompany(r.company());
        c.setJobTitle(r.jobTitle());
        c.setLinkedinUrl(r.linkedinUrl());
        c.setNotes(r.notes());
    }
}
