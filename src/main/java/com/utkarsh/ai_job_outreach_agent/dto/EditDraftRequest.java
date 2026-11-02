package com.utkarsh.ai_job_outreach_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record EditDraftRequest(@NotBlank String subject, @NotBlank String body) {}
