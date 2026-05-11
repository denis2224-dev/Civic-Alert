package com.civicalert.repository;

import com.civicalert.entity.DetectionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionLogRepository extends JpaRepository<DetectionLog, Long> {
    List<DetectionLog> findByReportIdOrderByCreatedAtDesc(Long reportId);
}

