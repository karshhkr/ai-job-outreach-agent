
package com.utkarsh.ai_job_outreach_agent.service;

import com.utkarsh.ai_job_outreach_agent.dto.ProfileRequest;
import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.repository.CandidateProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.utkarsh.ai_job_outreach_agent.dto.ProfileRequest;

import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private CandidateProfileRepository repository;

    @InjectMocks
    private ProfileService profileService;

    private CandidateProfile profile;

    @BeforeEach
    void setUp() {
        profile = new CandidateProfile();
        profile.setId(1L);
        profile.setFullName("Utkarsh Kumar Dabgarwal");
        profile.setHeadline("Java Backend Developer");
    }

    @Test
    void get_shouldReturnProfile_whenProfileExists() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(profile));

        CandidateProfile result = profileService.get();

        assertNotNull(result);
        assertEquals("Utkarsh Kumar Dabgarwal", result.getFullName());

        verify(repository).findById(1L);
    }

    @Test
    void get_shouldThrowException_whenProfileDoesNotExist() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.get()
        );

        verify(repository).findById(1L);
    }

    @Test
    void saveResumePath_shouldUpdateResumePath() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(profile));

        when(repository.save(any(CandidateProfile.class)))
                .thenReturn(profile);

        CandidateProfile result =
                profileService.saveResumePath("uploads/resume.pdf");

        assertEquals("uploads/resume.pdf", result.getResumePath());

        verify(repository).save(profile);
    }

    @Test
    void saveResumePath_shouldThrowException_whenProfileMissing() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.saveResumePath("uploads/resume.pdf")
        );

        verify(repository, never()).save(any());
    }
    @Test
    void save_shouldCreateNewProfile_whenProfileDoesNotExist() {

        // Arrange
        ProfileRequest request = new ProfileRequest(
                "Utkarsh Kumar Dabgarwal",
                "Java Backend Developer",
                "B.Tech CSE",
                "2025",
                "Java, Spring Boot, MySQL",
                "JobTracker, URL Shortener",
                "Noida, Gurugram, Bengaluru",
                "https://github.com/karshhkr",
                "https://linkedin.com/in/utkarsh"
        );

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        when(repository.save(any(CandidateProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CandidateProfile result = profileService.save(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Utkarsh Kumar Dabgarwal", result.getFullName());
        assertEquals("Java Backend Developer", result.getHeadline());
        assertEquals("2025", result.getGraduationYear());

        verify(repository).save(any(CandidateProfile.class));
    }
    @Test
    void save_shouldUpdateExistingProfile_whenProfileExists() {

        // Arrange
        ProfileRequest request = new ProfileRequest(
                "Utkarsh Kumar Dabgarwal",
                "Java Backend Engineer",
                "B.Tech CSE",
                "2025",
                "Java, Spring Boot, MySQL, Docker",
                "JobTracker, DevPilot",
                "Noida, Bengaluru",
                "https://github.com/karshhkr",
                "https://linkedin.com/in/utkarsh"
        );

        CandidateProfile existingProfile = new CandidateProfile();

        existingProfile.setId(1L);
        existingProfile.setFullName("Old Name");
        existingProfile.setHeadline("Old Headline");

        when(repository.findById(1L))
                .thenReturn(Optional.of(existingProfile));

        when(repository.save(any(CandidateProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CandidateProfile result = profileService.save(request);

        // Assert
        assertNotNull(result);

        assertEquals(1L, result.getId());
        assertEquals("Utkarsh Kumar Dabgarwal", result.getFullName());
        assertEquals("Java Backend Engineer", result.getHeadline());
        assertEquals("B.Tech CSE", result.getDegree());
        assertEquals("2025", result.getGraduationYear());
        assertEquals("Java, Spring Boot, MySQL, Docker", result.getSkills());
        assertEquals("JobTracker, DevPilot", result.getProjects());

        verify(repository).findById(1L);
        verify(repository).save(existingProfile);
    }
}
