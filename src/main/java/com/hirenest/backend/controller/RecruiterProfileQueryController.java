package com.hirenest.backend.controller;

import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.repository.RecruiterProfileRepository;
import com.hirenest.backend.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class RecruiterProfileQueryController {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;

    public RecruiterProfileQueryController(
            RecruiterProfileRepository recruiterProfileRepository,
            UserRepository userRepository) {
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/recruiter")
    public RecruiterProfile getRecruiter(Authentication authentication) {
        Long userId = requireUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == null
                || !"RECRUITER".equalsIgnoreCase(user.getRole().trim())) {
            throw new BadRequestException("Only recruiter accounts can access a recruiter profile");
        }

        List<RecruiterProfile> rows = recruiterProfileRepository.findAllByUserIdOrderByIdAsc(userId);
        if (rows.isEmpty()) {
            RecruiterProfile empty = new RecruiterProfile();
            empty.setCompanyName("");
            empty.setCompanyDescription("");
            return empty;
        }
        return rows.get(0);
    }

    private static Long requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new BadRequestException("Authentication required");
        }
        try {
            return Long.parseLong(principal.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid authenticated user id");
        }
    }
}
