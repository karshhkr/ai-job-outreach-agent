package com.utkarsh.ai_job_outreach_agent.controller;

import com.utkarsh.ai_job_outreach_agent.dto.HrContactRequest;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.service.HrContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class HrContactController {
    private final HrContactService service;
    public HrContactController(HrContactService service) { this.service = service; }

    @GetMapping public List<HrContact> all() { return service.findAll(); }
    @GetMapping("/{id}") public HrContact get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public HrContact create(@Valid @RequestBody HrContactRequest request) { return service.create(request); }
    @PutMapping("/{id}") public HrContact update(@PathVariable Long id, @Valid @RequestBody HrContactRequest request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
