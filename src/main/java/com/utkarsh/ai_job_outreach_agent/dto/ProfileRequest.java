package com.utkarsh.ai_job_outreach_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(
        @NotBlank String fullName,
        String headline,
        String degree,
        String graduationYear,
        String skills,
        String projects,
        String preferredLocations,
        String githubUrl,
        String linkedinUrl
) {}
