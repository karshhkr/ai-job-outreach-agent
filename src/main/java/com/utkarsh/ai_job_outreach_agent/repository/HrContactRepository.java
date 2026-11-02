package com.utkarsh.ai_job_outreach_agent.repository;

import com.utkarsh.ai_job_outreach_agent.entity.HrContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HrContactRepository extends JpaRepository<HrContact, Long> {
    Optional<HrContact> findByEmailIgnoreCase(String email);
}
