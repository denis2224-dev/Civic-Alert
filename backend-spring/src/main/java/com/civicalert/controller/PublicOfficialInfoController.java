package com.civicalert.controller;

import com.civicalert.entity.OfficialInfo;
import com.civicalert.service.OfficialInfoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicOfficialInfoController {

    private final OfficialInfoService officialInfoService;

    public PublicOfficialInfoController(OfficialInfoService officialInfoService) {
        this.officialInfoService = officialInfoService;
    }

    @GetMapping("/official-info")
    public List<OfficialInfo> getOfficialInfo() {
        return officialInfoService.getOfficialInfo();
    }
}

