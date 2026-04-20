package com.hirenest.backend.controller;

import com.hirenest.backend.dto.SkillSuggestionDtos;
import com.hirenest.backend.service.SkillSuggestionService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin
public class SkillSuggestionController {
    private final SkillSuggestionService skillSuggestionService;

    public SkillSuggestionController(SkillSuggestionService skillSuggestionService) {
        this.skillSuggestionService = skillSuggestionService;
    }

    @GetMapping("/suggestions/{userId}")
    public SkillSuggestionDtos.SkillSuggestionResponse getSuggestions(@PathVariable Long userId) {
        List<SkillSuggestionDtos.SkillSuggestion> suggestions = skillSuggestionService.suggestions(userId);
        SkillSuggestionDtos.SkillSuggestionResponse out = new SkillSuggestionDtos.SkillSuggestionResponse();
        out.suggestions = suggestions;
        return out;
    }

}


