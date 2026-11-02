package com.utkarsh.ai_job_outreach_agent.integration.gmail;

import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.enums.DraftStatus;
import com.utkarsh.ai_job_outreach_agent.repository.OutreachDraftRepository;
import com.utkarsh.ai_job_outreach_agent.service.ProfileService;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GmailEmailService {

    private final JavaMailSender mailSender;
    private final ProfileService profileService;
    private final OutreachDraftRepository drafts;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.email.daily-limit:20}")
    private int dailyLimit;

    public GmailEmailService(
            JavaMailSender mailSender,
            ProfileService profileService,
            OutreachDraftRepository drafts
    ) {
        this.mailSender = mailSender;
        this.profileService = profileService;
        this.drafts = drafts;
    }

    // Send one approved email
    @Transactional
    public OutreachDraft send(Long id) throws Exception {

        OutreachDraft draft = drafts.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Draft not found: " + id
                        )
                );

        if (draft.getStatus() != DraftStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED drafts can be sent."
            );
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException(
                    "GMAIL_USERNAME is not configured"
            );
        }

        if (draft.getContact().getEmail() == null ||
                draft.getContact().getEmail().isBlank()) {

            throw new IllegalStateException(
                    "Contact email is missing"
            );
        }

        CandidateProfile profile = profileService.get();

        if (profile.getResumePath() == null ||
                profile.getResumePath().isBlank()) {

            throw new IllegalStateException(
                    "Resume is not uploaded."
            );
        }

        File resume = new File(profile.getResumePath());

        if (!resume.exists() || !resume.isFile()) {
            throw new IllegalStateException(
                    "Resume file not found: " +
                            profile.getResumePath()
            );
        }

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(draft.getContact().getEmail());
        helper.setSubject(draft.getSubject());
        helper.setText(draft.getBody(), false);

        helper.addAttachment(
                "Resume.pdf",
                new FileSystemResource(resume)
        );

        // Send email through Gmail SMTP
        mailSender.send(message);

        // Update database after successful SMTP submission
        draft.setStatus(DraftStatus.SENT);
        draft.setSentAt(LocalDateTime.now());
        draft.setLastError(null);

        return drafts.save(draft);
    }

    // Send multiple approved emails with daily limit
    public Map<String, Object> sendApprovedBatch(int requestedSize)
            throws Exception {

        if (requestedSize < 1 || requestedSize > 5) {
            throw new IllegalArgumentException(
                    "Batch size must be between 1 and 5"
            );
        }

        if (dailyLimit < 1) {
            throw new IllegalStateException(
                    "Daily email limit must be greater than zero"
            );
        }

        LocalDateTime startOfDay =
                LocalDate.now().atStartOfDay();

        long sentToday = drafts.countByStatusAndSentAtGreaterThanEqual(
                DraftStatus.SENT,
                startOfDay
        );

        int remainingToday = (int) Math.max(
                0,
                dailyLimit - sentToday
        );

        int allowedSize = Math.min(
                requestedSize,
                remainingToday
        );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("dailyLimit", dailyLimit);
        response.put("sentTodayBeforeBatch", sentToday);

        if (allowedSize == 0) {

            response.put("sent", 0);
            response.put("failed", 0);
            response.put("remainingToday", 0);

            response.put(
                    "message",
                    "Daily sending limit reached"
            );

            response.put("results", List.of());

            return response;
        }

        List<OutreachDraft> approvedDrafts =
                drafts.findByStatusOrderByCreatedAtAsc(
                        DraftStatus.APPROVED,
                        PageRequest.of(0, allowedSize)
                );

        int sent = 0;
        int failed = 0;

        List<Map<String, Object>> results =
                new ArrayList<>();

        for (OutreachDraft draft : approvedDrafts) {

            Map<String, Object> item =
                    new LinkedHashMap<>();

            item.put("draftId", draft.getId());
            item.put(
                    "email",
                    draft.getContact().getEmail()
            );

            try {

                send(draft.getId());

                item.put("status", "SENT");
                sent++;

            } catch (Exception exception) {

                // Save the failure reason for future inspection
                draft.setLastError(
                        exception.getMessage() == null
                                ? "Email sending failed"
                                : exception.getMessage()
                );

                drafts.save(draft);

                item.put("status", "FAILED");
                item.put("error", draft.getLastError());

                failed++;
            }

            results.add(item);
        }

        response.put("sent", sent);
        response.put("failed", failed);

        response.put(
                "remainingToday",
                Math.max(0, remainingToday - sent)
        );

        response.put("results", results);

        return response;
    }
}