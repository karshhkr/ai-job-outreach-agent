
package com.utkarsh.ai_job_outreach_agent.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.repository.OutreachDraftRepository;
import com.utkarsh.ai_job_outreach_agent.service.ProfileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiDraftService {

    private final RestClient restClient;
    private final ProfileService profileService;
    private final OutreachDraftRepository drafts;
    private final ObjectMapper objectMapper;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-3.5-flash-lite}")
    private String model;

    public GeminiDraftService(
            RestClient.Builder builder,
            ProfileService profileService,
            OutreachDraftRepository drafts,
            ObjectMapper objectMapper,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
            String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.profileService = profileService;
        this.drafts = drafts;
        this.objectMapper = objectMapper;
    }

    public OutreachDraft generate(HrContact contact) {

        if (contact == null) {
            throw new IllegalArgumentException(
                    "HR contact cannot be null."
            );
        }

        if (contact.getId() == null) {
            throw new IllegalArgumentException(
                    "HR contact ID cannot be null."
            );
        }

        if (drafts.existsByContact_Id(contact.getId())) {
            throw new IllegalStateException(
                    "A draft already exists for this HR contact."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is missing."
            );
        }

        CandidateProfile profile = profileService.get();

        if (profile == null) {
            throw new IllegalStateException(
                    "Candidate profile not found."
            );
        }

        String prompt = buildPrompt(profile, contact);

        String generatedText = callGemini(prompt);

        String subject;
        String body;

        try {
            JsonNode json = objectMapper.readTree(generatedText);

            subject = json.path("subject").asText("").trim();
            body = json.path("body").asText("").trim();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Gemini returned an invalid JSON response.",
                    exception
            );
        }

        if (subject.isBlank()) {
            throw new IllegalStateException(
                    "Gemini response is missing email subject."
            );
        }

        if (body.isBlank()) {
            throw new IllegalStateException(
                    "Gemini response is missing email body."
            );
        }

        if (subject.length() > 255) {
            throw new IllegalStateException(
                    "Generated email subject is too long."
            );
        }

        OutreachDraft draft = new OutreachDraft();

        draft.setContact(contact);
        draft.setSubject(subject);
        draft.setBody(body);

        return drafts.save(draft);
    }

    private String buildPrompt(
            CandidateProfile profile,
            HrContact contact
    ) {

        try {
            String profileJson =
                    objectMapper.writeValueAsString(profile);

            String contactJson =
                    objectMapper.writeValueAsString(contact);

            return """
                    You are an expert professional job outreach email writer.

                    Create a personalized job inquiry email.

                    Candidate profile:
                    %s

                    HR contact:
                    %s

                    Instructions:
                    1. Write a professional and concise email.
                    2. The candidate is a 2025 graduate.
                    3. The candidate is seeking entry-level Java backend roles.
                    4. Do not invent employment experience.
                    5. Do not invent skills or achievements.
                    6. Mention relevant Java backend skills.
                    7. Do not claim that a resume is attached.
                    8. Return only valid JSON.
                    9. Include exactly two keys: subject and body.
                    10. Do not use Markdown code fences.

                    Expected format:
                    {
                      "subject": "Email subject",
                      "body": "Email body"
                    }
                    """.formatted(profileJson, contactJson);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not prepare Gemini prompt.",
                    exception
            );
        }
    }

    private String callGemini(String prompt) {

        Map<String, Object> request = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig",
                Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json"
                )
        );

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                JsonNode response = restClient.post()
                        .uri(
                                "/models/{model}:generateContent",
                                model
                        )
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(JsonNode.class);

                if (response == null) {
                    throw new IllegalStateException(
                            "Gemini returned an empty response."
                    );
                }

                String text = response.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText("");

                if (text.isBlank()) {
                    throw new IllegalStateException(
                            "Gemini returned empty content."
                    );
                }

                return text;

            } catch (RestClientResponseException exception) {

                if (exception.getStatusCode().value() != 503
                        || attempt == 3) {

                    throw new IllegalStateException(
                            "Gemini API request failed: "
                                    + exception.getStatusCode(),
                            exception
                    );
                }

                try {
                    Thread.sleep(2000L * attempt);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Gemini request retry interrupted.",
                            interruptedException
                    );
                }
            }
        }

        throw new IllegalStateException(
                "Gemini could not generate the email."
        );
    }
}
