package com.hirenest.backend.service;

import com.hirenest.backend.entity.Feedback;
import com.hirenest.backend.repository.FeedbackRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submit(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> byTarget(Long toUserId) {
        return feedbackRepository.findByToUserId(toUserId);
    }
}

