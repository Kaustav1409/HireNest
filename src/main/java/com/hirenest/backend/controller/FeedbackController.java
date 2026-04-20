package com.hirenest.backend.controller;

import com.hirenest.backend.entity.Feedback;
import com.hirenest.backend.service.FeedbackService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Feedback submitFeedback(@RequestBody Feedback payload) {
        return feedbackService.submit(payload);
    }

    @GetMapping("/{toUserId}")
    public List<Feedback> receivedFeedback(@PathVariable Long toUserId) {
        return feedbackService.byTarget(toUserId);
    }
}

