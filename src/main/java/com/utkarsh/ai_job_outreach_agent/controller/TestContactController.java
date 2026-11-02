package com.utkarsh.ai_job_outreach_agent.controller;

import com.utkarsh.ai_job_outreach_agent.dto.TestContactRequest;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.repository.HrContactRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-contacts")
public class TestContactController {

    private final HrContactRepository contacts;

    public TestContactController(HrContactRepository contacts) {
        this.contacts = contacts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HrContact create(@Valid @RequestBody TestContactRequest request) {

        if (contacts.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new IllegalStateException(
                    "A contact with this email already exists."
            );
        }

        HrContact contact = new HrContact();

        contact.setName(request.name());
        contact.setEmail(request.email().trim().toLowerCase());
        contact.setCompany("Personal Gmail Test");
        contact.setJobTitle("Java Backend Developer");
        contact.setNotes("Self-test contact. Do not treat as an HR lead.");

        return contacts.save(contact);
    }
}