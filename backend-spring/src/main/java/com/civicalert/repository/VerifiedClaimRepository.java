package com.civicalert.repository;

import com.civicalert.entity.VerifiedClaim;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerifiedClaimRepository extends JpaRepository<VerifiedClaim, Long> {
    List<VerifiedClaim> findByPublishedTrueOrderByUpdatedAtDesc();

    List<VerifiedClaim> findByPublishedTrueAndLanguageOrderByUpdatedAtDesc(String language);

    Optional<VerifiedClaim> findFirstByNormalizedClaimAndPublishedTrue(String normalizedClaim);

    Optional<VerifiedClaim> findFirstByNormalizedClaimAndLanguageAndPublishedTrue(
            String normalizedClaim,
            String language
    );

    Optional<VerifiedClaim> findFirstByNormalizedClaimAndLanguage(String normalizedClaim, String language);
}
