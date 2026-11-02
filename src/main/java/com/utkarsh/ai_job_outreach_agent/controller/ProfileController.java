package com.utkarsh.ai_job_outreach_agent.controller;

import com.utkarsh.ai_job_outreach_agent.dto.ProfileRequest;
import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.repository.CandidateProfileRepository;
import com.utkarsh.ai_job_outreach_agent.service.FileStorageService;
import com.utkarsh.ai_job_outreach_agent.service.ProfileService;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profiles;
    private final FileStorageService files;
    private final CandidateProfileRepository repository;

    public ProfileController(
            ProfileService profiles,
            FileStorageService files,
            CandidateProfileRepository repository
    ) {
        this.profiles = profiles;
        this.files = files;
        this.repository = repository;
    }

    @PutMapping
    public CandidateProfile save(@Valid @RequestBody ProfileRequest request) {
        return profiles.save(request);
    }

    @GetMapping
    public CandidateProfile get() {
        return profiles.get();
    }

    @PostMapping(
            value = "/resume",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, String> uploadResume(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        // Step 1: Check whether candidate profile exists
        CandidateProfile profile = repository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Please save your candidate profile first."
                ));

        // Step 2: Save resume in uploads folder
        String resumePath = files.saveResume(file);

        // Step 3: Store resume path in candidate_profile table
        profile.setResumePath(resumePath);
        repository.save(profile);

        // Step 4: Return confirmation
        return Map.of(
                "message", "Resume uploaded and linked to candidate profile successfully",
                "resumePath", resumePath
        );
    }
}