package com.utkarsh.ai_job_outreach_agent.entity;

import com.utkarsh.ai_job_outreach_agent.enums.DraftStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outreach_drafts")
public class OutreachDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private HrContact contact;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 12000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @Column(length = 1000)
    private String lastError;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public HrContact getContact() {
        return contact;
    }

    public void setContact(HrContact contact) {
        this.contact = contact;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}