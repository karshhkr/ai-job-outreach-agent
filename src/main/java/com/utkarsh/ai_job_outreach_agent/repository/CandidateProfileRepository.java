package com.utkarsh.ai_job_outreach_agent.repository;

import com.utkarsh.ai_job_outreach_agent.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {}
