package com.utkarsh.ai_job_outreach_agent.repository;

import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.enums.DraftStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutreachDraftRepository
        extends JpaRepository<OutreachDraft, Long> {

    // Find all drafts belonging to contact IDs
    List<OutreachDraft> findByContact_IdIn(List<Long> contactIds);

    // Fetch drafts ordered by creation date
    List<OutreachDraft> findAllByOrderByCreatedAtDesc();

    // Check whether a draft already exists for a contact
    boolean existsByContact_Id(Long contactId);

    // Fetch approved drafts for batch sending
    List<OutreachDraft> findByStatusOrderByCreatedAtAsc(
            DraftStatus status,
            Pageable pageable
    );

    // Count emails sent today
    long countByStatusAndSentAtGreaterThanEqual(
            DraftStatus status,
            LocalDateTime startOfDay
    );
}