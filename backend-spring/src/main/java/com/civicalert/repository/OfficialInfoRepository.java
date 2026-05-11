package com.civicalert.repository;

import com.civicalert.entity.OfficialInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialInfoRepository extends JpaRepository<OfficialInfo, Long> {
    Optional<OfficialInfo> findByTopic(String topic);
}

