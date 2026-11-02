package com.utkarsh.ai_job_outreach_agent.controller;

import com.utkarsh.ai_job_outreach_agent.dto.EditDraftRequest;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.enums.DraftStatus;
import com.utkarsh.ai_job_outreach_agent.integration.ai.GeminiDraftService;
import com.utkarsh.ai_job_outreach_agent.integration.gmail.GmailEmailService;
import com.utkarsh.ai_job_outreach_agent.repository.HrContactRepository;
import com.utkarsh.ai_job_outreach_agent.repository.OutreachDraftRepository;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drafts")
public class OutreachDraftController {

    private final GeminiDraftService ai;
    private final GmailEmailService email;
    private final HrContactRepository contacts;
    private final OutreachDraftRepository drafts;

    public OutreachDraftController(
            GeminiDraftService ai,
            GmailEmailService email,
            HrContactRepository contacts,
            OutreachDraftRepository drafts
    ) {
        this.ai = ai;
        this.email = email;
        this.contacts = contacts;
        this.drafts = drafts;
    }

    // Generate one draft
    @PostMapping("/generate/{contactId}")
    public OutreachDraft generate(
            @PathVariable Long contactId
    ) {

        HrContact contact = contacts.findById(contactId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Contact not found: " + contactId
                        )
                );

        return ai.generate(contact);
    }

    // Generate drafts in batches
    @PostMapping("/generate-batch")
    public Map<String, Object> generateBatch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page cannot be negative"
            );
        }

        if (size < 1 || size > 20) {
            throw new IllegalArgumentException(
                    "Batch size must be between 1 and 20"
            );
        }

        Page<HrContact> contactPage = contacts.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );

        List<HrContact> batchContacts =
                contactPage.getContent();

        List<Long> contactIds = batchContacts.stream()
                .map(HrContact::getId)
                .toList();

        Set<Long> alreadyGeneratedIds = new HashSet<>();

        if (!contactIds.isEmpty()) {

            alreadyGeneratedIds = drafts
                    .findByContact_IdIn(contactIds)
                    .stream()
                    .map(draft ->
                            draft.getContact().getId()
                    )
                    .collect(Collectors.toSet());
        }

        int generated = 0;
        int skipped = 0;

        List<Map<String, Object>> errors =
                new ArrayList<>();

        for (HrContact contact : batchContacts) {

            if (alreadyGeneratedIds.contains(contact.getId())) {
                skipped++;
                continue;
            }

            if (contact.getEmail() == null ||
                    contact.getEmail().isBlank() ||
                    !contact.getEmail().contains("@")) {

                skipped++;
                continue;
            }

            try {

                ai.generate(contact);
                generated++;

            } catch (Exception exception) {

                Map<String, Object> error =
                        new HashMap<>();

                error.put("contactId", contact.getId());
                error.put("email", contact.getEmail());
                error.put("error", exception.getMessage());

                errors.add(error);
            }
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("page", page);
        result.put("batchSize", size);
        result.put(
                "totalContacts",
                contactPage.getTotalElements()
        );

        result.put("generated", generated);
        result.put("skipped", skipped);
        result.put("failed", errors.size());

        result.put("hasNext", contactPage.hasNext());

        result.put(
                "nextPage",
                contactPage.hasNext() ? page + 1 : null
        );

        result.put("errors", errors);

        return result;
    }

    // Fetch all drafts
    @GetMapping
    public List<OutreachDraft> all() {
        return drafts.findAllByOrderByCreatedAtDesc();
    }

    // Edit draft
    @PutMapping("/{id}")
    public OutreachDraft edit(
            @PathVariable Long id,
            @Valid @RequestBody EditDraftRequest request
    ) {

        OutreachDraft draft = getDraft(id);

        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT items can be edited"
            );
        }

        draft.setSubject(request.subject());
        draft.setBody(request.body());

        return drafts.save(draft);
    }

    // Approve draft
    @PostMapping("/{id}/approve")
    public OutreachDraft approve(
            @PathVariable Long id
    ) {

        OutreachDraft draft = getDraft(id);

        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT items can be approved"
            );
        }

        draft.setStatus(DraftStatus.APPROVED);

        return drafts.save(draft);
    }

    // Reject draft
    @PostMapping("/{id}/reject")
    public OutreachDraft reject(
            @PathVariable Long id
    ) {

        OutreachDraft draft = getDraft(id);

        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT items can be rejected"
            );
        }

        draft.setStatus(DraftStatus.REJECTED);

        return drafts.save(draft);
    }

    // Send one approved email
    @PostMapping("/{id}/send")
    public OutreachDraft send(
            @PathVariable Long id
    ) throws Exception {

        return email.send(id);
    }

    // Send approved emails in a controlled batch
    @PostMapping("/send-batch")
    public Map<String, Object> sendBatch(
            @RequestParam(defaultValue = "5") int size
    ) throws Exception {

        return email.sendApprovedBatch(size);
    }

    private OutreachDraft getDraft(Long id) {

        return drafts.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Draft not found: " + id
                        )
                );
    }
}