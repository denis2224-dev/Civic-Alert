package com.civicalert.repository;

import com.civicalert.entity.RumorPattern;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RumorPatternRepository extends JpaRepository<RumorPattern, Long> {
    List<RumorPattern> findByActiveTrue();
}

