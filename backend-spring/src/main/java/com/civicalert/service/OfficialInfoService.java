package com.civicalert.service;

import com.civicalert.entity.OfficialInfo;
import com.civicalert.repository.OfficialInfoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OfficialInfoService {

    private final OfficialInfoRepository officialInfoRepository;

    public OfficialInfoService(OfficialInfoRepository officialInfoRepository) {
        this.officialInfoRepository = officialInfoRepository;
    }

    public List<OfficialInfo> getOfficialInfo() {
        return officialInfoRepository.findAllByOrderByUpdatedAtDesc();
    }
}

