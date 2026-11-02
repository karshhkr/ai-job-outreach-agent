package com.utkarsh.ai_job_outreach_agent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestContactRequest(

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email

) {
}