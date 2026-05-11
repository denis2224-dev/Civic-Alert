package com.civicalert.repository;

import com.civicalert.entity.PublicReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicReportRepository extends JpaRepository<PublicReport, Long> {
    Optional<PublicReport> findByTrackingCode(String trackingCode);

    List<PublicReport> findAllByOrderByCreatedAtDesc();
}

