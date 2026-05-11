package com.civicalert.repository;

import com.civicalert.entity.VerifiedClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerifiedClaimRepository extends JpaRepository<VerifiedClaim, Long> {
    List<VerifiedClaim> findByPublishedTrueOrderByUpdatedAtDesc();

    List<VerifiedClaim> findByPublishedTrueAndLanguageOrderByUpdatedAtDesc(String language);
}

