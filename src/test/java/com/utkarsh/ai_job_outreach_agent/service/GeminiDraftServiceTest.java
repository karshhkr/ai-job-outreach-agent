
package com.utkarsh.ai_job_outreach_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import com.utkarsh.ai_job_outreach_agent.entity.OutreachDraft;
import com.utkarsh.ai_job_outreach_agent.integration.ai.GeminiDraftService;
import com.utkarsh.ai_job_outreach_agent.repository.OutreachDraftRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class GeminiDraftServiceTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private OutreachDraftRepository drafts;

    private GeminiDraftService geminiDraftService;

    private MockRestServiceServer mockServer;

    private HrContact contact;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(builder)
                .build();

        geminiDraftService = new GeminiDraftService(
                builder,
                profileService,
                drafts,
                objectMapper,
                "http://localhost:9999"
        );

        ReflectionTestUtils.setField(
                geminiDraftService,
                "apiKey",
                "test-api-key"
        );

        ReflectionTestUtils.setField(
                geminiDraftService,
                "model",
                "test-model"
        );

        contact = new HrContact();

        contact.setId(10L);
        contact.setName("Rahul Sharma");
        contact.setEmail("rahul@example.com");
        contact.setCompany("Google");
        contact.setJobTitle("HR Manager");
        contact.setNotes("Java backend hiring");
    }

    private String createGeminiResponse(String innerJson)
            throws Exception {

        Map<String, Object> response = Map.of(
                "candidates",
                List.of(
                        Map.of(
                                "content",
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of("text", innerJson)
                                        )
                                )
                        )
                )
        );

        return objectMapper.writeValueAsString(response);
    }

    @Test
    void generate_shouldSaveDraft_whenGeminiReturnsValidResponse()
            throws Exception {

        when(drafts.existsByContact_Id(10L))
                .thenReturn(false);

        when(profileService.get())
                .thenReturn(new CandidateProfile());

        String innerJson = objectMapper.writeValueAsString(
                Map.of(
                        "subject",
                        "Java Backend Opportunity",
                        "body",
                        "Hello Rahul, I am interested in entry-level Java backend opportunities at Google.\nRegards, Candidate"
                )
        );

        String geminiResponse =
                createGeminiResponse(innerJson);

        mockServer.expect(requestTo(
                        "http://localhost:9999/models/test-model:generateContent"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        "x-goog-api-key",
                        "test-api-key"
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andRespond(withSuccess(
                        geminiResponse,
                        MediaType.APPLICATION_JSON
                ));

        when(drafts.save(any(OutreachDraft.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutreachDraft result =
                geminiDraftService.generate(contact);

        assertNotNull(result);

        assertEquals(
                "Java Backend Opportunity",
                result.getSubject()
        );

        assertTrue(
                result.getBody().contains("Hello Rahul")
        );

        assertEquals(contact, result.getContact());

        verify(drafts).save(any(OutreachDraft.class));
        verify(profileService).get();

        mockServer.verify();
    }

    @Test
    void generate_shouldThrowException_whenContactIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> geminiDraftService.generate(null)
        );

        verifyNoInteractions(drafts);
        verifyNoInteractions(profileService);
    }

    @Test
    void generate_shouldThrowException_whenDraftAlreadyExists() {

        when(drafts.existsByContact_Id(10L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> geminiDraftService.generate(contact)
        );

        verify(drafts, never())
                .save(any(OutreachDraft.class));

        verifyNoInteractions(profileService);
    }

    @Test
    void generate_shouldThrowException_whenApiKeyIsMissing() {

        ReflectionTestUtils.setField(
                geminiDraftService,
                "apiKey",
                ""
        );

        when(drafts.existsByContact_Id(10L))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> geminiDraftService.generate(contact)
        );

        verifyNoInteractions(profileService);

        verify(drafts, never())
                .save(any(OutreachDraft.class));
    }

    @Test
    void generate_shouldThrowException_whenGeminiReturnsEmptyContent()
            throws Exception {

        when(drafts.existsByContact_Id(10L))
                .thenReturn(false);

        when(profileService.get())
                .thenReturn(new CandidateProfile());

        String geminiResponse =
                createGeminiResponse("");

        mockServer.expect(requestTo(
                        "http://localhost:9999/models/test-model:generateContent"
                ))
                .andRespond(withSuccess(
                        geminiResponse,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                IllegalStateException.class,
                () -> geminiDraftService.generate(contact)
        );

        verify(drafts, never())
                .save(any(OutreachDraft.class));

        mockServer.verify();
    }

    @Test
    void generate_shouldThrowException_whenSubjectIsMissing()
            throws Exception {

        when(drafts.existsByContact_Id(10L))
                .thenReturn(false);

        when(profileService.get())
                .thenReturn(new CandidateProfile());

        String innerJson = objectMapper.writeValueAsString(
                Map.of(
                        "body",
                        "Hello Rahul, I am interested in Java backend opportunities."
                )
        );

        String geminiResponse =
                createGeminiResponse(innerJson);

        mockServer.expect(requestTo(
                        "http://localhost:9999/models/test-model:generateContent"
                ))
                .andRespond(withSuccess(
                        geminiResponse,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                IllegalStateException.class,
                () -> geminiDraftService.generate(contact)
        );

        verify(drafts, never())
                .save(any(OutreachDraft.class));

        mockServer.verify();
    }
}
