package com.hirenest.backend.controller;

import com.hirenest.backend.dto.JobDtos.MatchedJobResponse;
import com.hirenest.backend.dto.JobDtos.MatchingFilterRequest;
import com.hirenest.backend.service.MatchingService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MatchingController {
    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/matching/{userId}")
    public List<MatchedJobResponse> matching(
            @PathVariable Long userId,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String keyword
    ) {
        MatchingFilterRequest filter = new MatchingFilterRequest();
        filter.minSalary = minSalary;
        filter.maxSalary = maxSalary;
        filter.company = company;
        filter.remote = remote;
        filter.location = location;
        filter.keyword = keyword;
        return matchingService.matching(userId, filter);
    }
}

