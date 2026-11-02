
package com.utkarsh.ai_job_outreach_agent.service;

import com.utkarsh.ai_job_outreach_agent.dto.ProfileRequest;
import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import com.utkarsh.ai_job_outreach_agent.repository.CandidateProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CandidateProfileRepository repository;

    public ProfileService(CandidateProfileRepository repository) {
        this.repository = repository;
    }

    public CandidateProfile get() {
        return repository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Candidate profile not configured. Call PUT /api/profile first."
                ));
    }

    @Transactional
    public CandidateProfile save(ProfileRequest r) {

        CandidateProfile p = repository.findById(1L)
                .orElseGet(CandidateProfile::new);

        p.setId(1L);
        p.setFullName(r.fullName());
        p.setHeadline(r.headline());
        p.setDegree(r.degree());
        p.setGraduationYear(r.graduationYear());
        p.setSkills(r.skills());
        p.setProjects(r.projects());
        p.setPreferredLocations(r.preferredLocations());
        p.setGithubUrl(r.githubUrl());
        p.setLinkedinUrl(r.linkedinUrl());

        return repository.save(p);
    }

    // Save uploaded resume path in database
    @Transactional
    public CandidateProfile saveResumePath(String resumePath) {

        CandidateProfile profile = repository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Candidate profile not found. Save profile first."
                ));

        profile.setResumePath(resumePath);

        return repository.save(profile);
    }
}
