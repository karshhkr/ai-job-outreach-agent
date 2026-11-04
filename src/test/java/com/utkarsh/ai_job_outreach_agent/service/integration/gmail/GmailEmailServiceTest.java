
package com.utkarsh.ai_job_outreach_agent.service.integration.gmail;

import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.enums.DraftStatus;
import com.utkarsh.ai_job_outreach_agent.integration.gmail.GmailEmailService;
import com.utkarsh.ai_job_outreach_agent.repository.OutreachDraftRepository;
import com.utkarsh.ai_job_outreach_agent.service.ProfileService;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GmailEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ProfileService profileService;

    @Mock
    private OutreachDraftRepository drafts;

    private GmailEmailService gmailEmailService;

    private OutreachDraft draft;

    private CandidateProfile profile;

    private MimeMessage mimeMessage;

    @TempDir
    Path tempDir;

    private Path resumePath;

    @BeforeEach
    void setUp() throws Exception {

        gmailEmailService = new GmailEmailService(
                mailSender,
                profileService,
                drafts
        );

        ReflectionTestUtils.setField(
                gmailEmailService,
                "fromAddress",
                "test@gmail.com"
        );

        ReflectionTestUtils.setField(
                gmailEmailService,
                "dailyLimit",
                20
        );

        resumePath = tempDir.resolve("resume.pdf");

        Files.write(
                resumePath,
                "%PDF-1.4 test resume".getBytes()
        );

        profile = new CandidateProfile();
        profile.setResumePath(resumePath.toString());

        HrContact contact = new HrContact();

        contact.setId(10L);
        contact.setName("Rahul Sharma");
        contact.setEmail("rahul@example.com");
        contact.setCompany("Google");
        contact.setJobTitle("HR Manager");

        draft = new OutreachDraft();

        // OutreachDraft entity has no setId().
        // Set ID through ReflectionTestUtils for repository mocking.
        ReflectionTestUtils.setField(draft, "id", 1L);

        draft.setContact(contact);
        draft.setSubject("Java Backend Developer Opportunity");
        draft.setBody(
                "Hello Rahul, I am interested in Java backend opportunities."
        );
        draft.setStatus(DraftStatus.APPROVED);

        mimeMessage = new MimeMessage(
                Session.getInstance(new Properties())
        );
    }

    // 1. Successfully send an approved email

    @Test
    void send_shouldSendEmail_whenDraftIsApproved()
            throws Exception {

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        when(profileService.get())
                .thenReturn(profile);

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        when(drafts.save(any(OutreachDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutreachDraft result = gmailEmailService.send(1L);

        assertNotNull(result);

        assertEquals(DraftStatus.SENT, result.getStatus());

        assertNotNull(result.getSentAt());

        assertNull(result.getLastError());

        assertEquals(
                "Java Backend Developer Opportunity",
                result.getSubject()
        );

        verify(mailSender).send(mimeMessage);

        verify(profileService).get();

        verify(drafts).save(draft);
    }

    // 2. Reject an unapproved draft

    @Test
    void send_shouldThrowException_whenDraftIsNotApproved() {

        draft.setStatus(DraftStatus.DRAFT);

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        assertThrows(
                IllegalStateException.class,
                () -> gmailEmailService.send(1L)
        );

        verifyNoInteractions(profileService);

        verify(mailSender, never())
                .send(any(MimeMessage.class));

        verify(drafts, never())
                .save(any(OutreachDraft.class));
    }

    // 3. Draft does not exist

    @Test
    void send_shouldThrowException_whenDraftDoesNotExist() {

        when(drafts.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> gmailEmailService.send(1L)
        );

        verifyNoInteractions(profileService);

        verifyNoInteractions(mailSender);
    }

    // 4. Gmail username is missing

    @Test
    void send_shouldThrowException_whenFromAddressIsMissing() {

        ReflectionTestUtils.setField(
                gmailEmailService,
                "fromAddress",
                ""
        );

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        assertThrows(
                IllegalStateException.class,
                () -> gmailEmailService.send(1L)
        );

        verifyNoInteractions(profileService);

        verify(mailSender, never())
                .send(any(MimeMessage.class));
    }

    // 5. Contact email is missing

    @Test
    void send_shouldThrowException_whenContactEmailIsMissing() {

        draft.getContact().setEmail("");

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        assertThrows(
                IllegalStateException.class,
                () -> gmailEmailService.send(1L)
        );

        verifyNoInteractions(profileService);

        verify(mailSender, never())
                .send(any(MimeMessage.class));
    }

    // 6. Resume path is missing

    @Test
    void send_shouldThrowException_whenResumePathIsMissing() {

        profile.setResumePath("");

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        when(profileService.get())
                .thenReturn(profile);

        assertThrows(
                IllegalStateException.class,
                () -> gmailEmailService.send(1L)
        );

        verify(mailSender, never())
                .send(any(MimeMessage.class));

        verify(drafts, never())
                .save(any(OutreachDraft.class));
    }

    // 7. Resume file does not exist

    @Test
    void send_shouldThrowException_whenResumeFileDoesNotExist() {

        profile.setResumePath(
                tempDir.resolve("missing-resume.pdf").toString()
        );

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        when(profileService.get())
                .thenReturn(profile);

        assertThrows(
                IllegalStateException.class,
                () -> gmailEmailService.send(1L)
        );

        verify(mailSender, never())
                .send(any(MimeMessage.class));

        verify(drafts, never())
                .save(any(OutreachDraft.class));
    }

    // 8. Daily sending limit is reached

    @Test
    void sendApprovedBatch_shouldNotSend_whenDailyLimitIsReached()
            throws Exception {

        when(drafts.countByStatusAndSentAtGreaterThanEqual(
                eq(DraftStatus.SENT),
                any(LocalDateTime.class)
        )).thenReturn(20L);

        Map<String, Object> result =
                gmailEmailService.sendApprovedBatch(5);

        assertEquals(20, result.get("dailyLimit"));

        assertEquals(20L, result.get("sentTodayBeforeBatch"));

        assertEquals(0, result.get("sent"));

        assertEquals(0, result.get("failed"));

        assertEquals(0, result.get("remainingToday"));

        assertEquals(
                "Daily sending limit reached",
                result.get("message")
        );

        verify(drafts, never())
                .findByStatusOrderByCreatedAtAsc(
                        eq(DraftStatus.APPROVED),
                        any()
                );

        verifyNoInteractions(mailSender);
    }

    // 9. Successfully send an approved batch

    @Test
    void sendApprovedBatch_shouldSendApprovedDrafts()
            throws Exception {

        when(drafts.countByStatusAndSentAtGreaterThanEqual(
                eq(DraftStatus.SENT),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        when(drafts.findByStatusOrderByCreatedAtAsc(
                eq(DraftStatus.APPROVED),
                any()
        )).thenReturn(List.of(draft));

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        when(profileService.get())
                .thenReturn(profile);

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        when(drafts.save(any(OutreachDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result =
                gmailEmailService.sendApprovedBatch(5);

        assertEquals(20, result.get("dailyLimit"));

        assertEquals(0L, result.get("sentTodayBeforeBatch"));

        assertEquals(1, result.get("sent"));

        assertEquals(0, result.get("failed"));

        assertEquals(19, result.get("remainingToday"));

        assertEquals(DraftStatus.SENT, draft.getStatus());

        verify(mailSender).send(mimeMessage);

        verify(drafts).save(draft);
    }

    // 10. Invalid batch size should be rejected

    @Test
    void sendApprovedBatch_shouldThrowException_whenBatchSizeIsInvalid() {

        assertThrows(
                IllegalArgumentException.class,
                () -> gmailEmailService.sendApprovedBatch(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> gmailEmailService.sendApprovedBatch(6)
        );

        verifyNoInteractions(drafts);

        verifyNoInteractions(mailSender);
    }

    // 11. Batch should record failure when email cannot be sent

    @Test
    void sendApprovedBatch_shouldRecordFailure_whenResumeIsMissing()
            throws Exception {

        profile.setResumePath("");

        when(drafts.countByStatusAndSentAtGreaterThanEqual(
                eq(DraftStatus.SENT),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        when(drafts.findByStatusOrderByCreatedAtAsc(
                eq(DraftStatus.APPROVED),
                any()
        )).thenReturn(List.of(draft));

        when(drafts.findById(1L))
                .thenReturn(Optional.of(draft));

        when(profileService.get())
                .thenReturn(profile);

        when(drafts.save(any(OutreachDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result =
                gmailEmailService.sendApprovedBatch(5);

        assertEquals(0, result.get("sent"));

        assertEquals(1, result.get("failed"));

        assertEquals(
                "FAILED",
                ((Map<?, ?>) ((List<?>) result.get("results")).get(0))
                        .get("status")
        );

        assertNotNull(draft.getLastError());

        verify(mailSender, never())
                .send(any(MimeMessage.class));

        verify(drafts).save(draft);
    }
}
