package com.hirenest.backend.controller;

import com.hirenest.backend.dto.ProfileDtos.CandidateProfileRequest;
import com.hirenest.backend.dto.ProfileDtos.CandidateProfileResponse;
import com.hirenest.backend.dto.ProfileDtos.RecruiterProfileRequest;
import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.service.ProfileService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/candidate")
    public CandidateProfileResponse saveCandidate(@RequestBody CandidateProfileRequest request) {
        return profileService.saveCandidate(request);
    }

    @GetMapping("/candidate/{userId}")
    public CandidateProfileResponse getCandidate(@PathVariable Long userId) {
        return profileService.toCandidateProfileResponse(userId, profileService.getCandidateOrDefault(userId));
    }

    @PostMapping(value = "/candidate/{userId}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadCandidateResume(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        return profileService.uploadCandidateResume(userId, file);
    }

    @GetMapping("/candidate/{userId}/resume")
    public ResponseEntity<Resource> downloadCandidateResume(@PathVariable Long userId) {
        Map<String, Object> meta = profileService.getCandidateResumeMeta(userId);
        Path path = Path.of(String.valueOf(meta.get("resumePath")));
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = sanitizeResumeDownloadFilename(String.valueOf(meta.getOrDefault("fileName", "resume.pdf")));
        String contentType = "application/pdf";
        try {
            String detected = Files.probeContentType(path);
            if (detected != null) {
                contentType = detected;
            }
        } catch (Exception ignored) {
            // keep default
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /** Strip path/control characters so Content-Disposition cannot break or escape the download name. */
    private static String sanitizeResumeDownloadFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "resume.pdf";
        }
        String s = raw.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        return s.isBlank() ? "resume.pdf" : s;
    }

    @PostMapping("/recruiter")
    public RecruiterProfile saveRecruiter(
            @RequestBody RecruiterProfileRequest request,
            Authentication authentication) {
        Long authenticatedUserId = requireUserId(authentication);
        if (request != null && request.userId != null && !request.userId.equals(authenticatedUserId)) {
            throw new BadRequestException("userId does not match authenticated user");
        }
        return profileService.saveRecruiter(authenticatedUserId, request);
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

