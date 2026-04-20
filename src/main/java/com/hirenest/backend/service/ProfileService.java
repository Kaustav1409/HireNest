package com.hirenest.backend.service;

import com.hirenest.backend.dto.ProfileDtos.CandidateProfileRequest;
import com.hirenest.backend.dto.ProfileDtos.CandidateProfileResponse;
import com.hirenest.backend.dto.ProfileDtos.RecruiterProfileRequest;
import com.hirenest.backend.entity.CandidateProfile;
import com.hirenest.backend.entity.RecruiterProfile;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.exception.BadRequestException;
import com.hirenest.backend.exception.NotFoundException;
import com.hirenest.backend.repository.CandidateProfileRepository;
import com.hirenest.backend.repository.RecruiterProfileRepository;
import com.hirenest.backend.repository.UserRepository;
import com.hirenest.backend.util.DataIntegrityMessageMapper;
import com.hirenest.backend.util.ResumeParserUtil;
import com.hirenest.backend.util.ResumeProfileInference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;
    private static final long MAX_RESUME_SIZE_BYTES = 10L * 1024L * 1024L; // 10MB
    private final Path resumeDir = Path.of("uploads", "resumes").toAbsolutePath().normalize();

    public ProfileService(CandidateProfileRepository candidateProfileRepository,
                          RecruiterProfileRepository recruiterProfileRepository,
                          UserRepository userRepository) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CandidateProfileResponse saveCandidate(CandidateProfileRequest request) {
        Long safeUserId = Objects.requireNonNull(request.userId(), "userId is required");
        User user = userRepository.findById(safeUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CandidateProfile profile = candidateProfileRepository.findByUserId(safeUserId).orElseGet(CandidateProfile::new);
        profile.setUser(user);
        String preservedExtracted = profile.getExtractedSkills();
        String preservedResumePath = profile.getResumePath();
        String preservedResumeName = profile.getResumeFileName();
        LocalDateTime preservedResumeAt = profile.getResumeUploadedAt();
        profile.setSkills(nullToEmpty(request.skills()));
        profile.setExperienceYears(request.experienceYears());
        profile.setExperienceLevel(request.experienceLevel() != null ? request.experienceLevel().trim() : "");
        profile.setBio(nullToEmpty(request.bio()));
        profile.setPreferredRoles(nullToEmpty(request.preferredRoles()));
        profile.setLocation(nullToEmpty(request.location()));
        boolean remote = request.remotePreferred() != null && request.remotePreferred();
        profile.setRemotePreferred(remote);
        if (preservedExtracted != null && !preservedExtracted.isBlank()) {
            profile.setExtractedSkills(preservedExtracted);
        } else if (profile.getExtractedSkills() == null) {
            profile.setExtractedSkills("");
        }
        if (preservedResumePath != null && !preservedResumePath.isBlank()) {
            profile.setResumePath(preservedResumePath);
        }
        if (preservedResumeName != null && !preservedResumeName.isBlank()) {
            profile.setResumeFileName(preservedResumeName);
        }
        if (preservedResumeAt != null) {
            profile.setResumeUploadedAt(preservedResumeAt);
        }
        CandidateProfile saved = candidateProfileRepository.saveAndFlush(profile);
        return toCandidateProfileResponse(safeUserId, saved);
    }

    /** Maps entity to API DTO without mutating the entity (safe under OSIV). */
    public CandidateProfileResponse toCandidateProfileResponse(Long userId, CandidateProfile profile) {
        if (profile == null) {
            return emptyCandidateProfileResponse(userId);
        }
        Long resolvedUserId = profile.getUser() != null ? profile.getUser().getId() : userId;
        String resumeName = resolveResumeFileName(profile);
        return new CandidateProfileResponse(
                profile.getId(),
                resolvedUserId,
                nz(profile.getSkills()),
                nz(profile.getExtractedSkills()),
                profile.getExperienceYears(),
                nz(profile.getExperienceLevel()),
                nz(profile.getBio()),
                nz(profile.getPreferredRoles()),
                nz(profile.getLocation()),
                Boolean.TRUE.equals(profile.getRemotePreferred()),
                resumeName,
                profile.getResumeUploadedAt());
    }

    public CandidateProfileResponse emptyCandidateProfileResponse(Long userId) {
        return new CandidateProfileResponse(
                null,
                userId,
                "",
                "",
                0,
                "",
                "",
                "",
                "",
                false,
                null,
                null);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Loads the persisted profile or an in-memory default. Does <b>not</b> normalize null fields on a managed
     * entity â€” mutating a managed {@code CandidateProfile} during a web request can flush empty strings to
     * the database under open-session-in-view.
     */
    public CandidateProfile getCandidateOrDefault(Long userId) {
        return candidateProfileRepository.findByUserId(userId).orElseGet(() -> {
            CandidateProfile empty = new CandidateProfile();
            empty.setSkills("");
            empty.setExtractedSkills("");
            empty.setExperienceYears(0);
            empty.setExperienceLevel("");
            empty.setBio("");
            empty.setPreferredRoles("");
            empty.setLocation("");
            empty.setRemotePreferred(false);
            return empty;
        });
    }

    /**
     * Upsert recruiter profile for the authenticated recruiter user id (from JWT).
     * {@code request.userId} is optional; when present it must match the authenticated id (enforced in controller).
     */
    @Transactional
    public RecruiterProfile saveRecruiter(Long userId, RecruiterProfileRequest request) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String role = user.getRole();
        if (role == null || !"RECRUITER".equalsIgnoreCase(role.trim())) {
            throw new BadRequestException("Only recruiter accounts can save a recruiter profile");
        }
        String companyName = request.companyName == null ? "" : request.companyName.trim();
        if (companyName.isEmpty()) {
            throw new BadRequestException("Company name is required");
        }
        String companyDescription = request.companyDescription == null ? "" : request.companyDescription.trim();

        List<RecruiterProfile> existing = recruiterProfileRepository.findAllByUserIdOrderByIdAsc(userId);
        RecruiterProfile profile;
        if (existing.isEmpty()) {
            profile = new RecruiterProfile();
        } else {
            profile = existing.get(0);
            if (existing.size() > 1) {
                log.error(
                        "Data problem: {} recruiter_profile rows for userId={}; upserting id={}. Remove duplicate rows in DB.",
                        existing.size(),
                        userId,
                        profile.getId());
            }
        }
        boolean isUpdate = profile.getId() != null;
        if (user.getId() == null) {
            throw new BadRequestException("Could not resolve recruiter user (missing id)");
        }
        profile.setUser(user);
        profile.setCompanyName(companyName);
        profile.setCompanyDescription(companyDescription);
        try {
            // Flush here so SQL runs inside this try/catch. A plain save() often flushes at transaction
            // commit; then DataIntegrityViolationException escapes after the method returns and never maps
            // to a clear API message (user only sees a generic "Database constraint failed").
            RecruiterProfile saved = recruiterProfileRepository.saveAndFlush(profile);
            log.debug("{} recruiter profile for userId={} (profileId={})",
                    isUpdate ? "Updated" : "Created", userId, saved.getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.error(
                    "Recruiter profile save integrity failure userId={} update={} class={} messages={}",
                    userId,
                    isUpdate,
                    ex.getClass().getName(),
                    DataIntegrityMessageMapper.collectAllMessages(ex),
                    ex);
            throw new BadRequestException(DataIntegrityMessageMapper.toClientMessage(ex, "Recruiter profile save"));
        }
    }

    @Transactional
    public Map<String, Object> uploadCandidateResume(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a PDF file to upload");
        }
        if (file.getSize() > MAX_RESUME_SIZE_BYTES) {
            throw new BadRequestException("Resume size exceeds 10MB limit");
        }

        String rawOriginalName = file.getOriginalFilename();
        String originalName = rawOriginalName != null ? rawOriginalName : "resume.pdf";
        boolean extensionPdf = originalName.toLowerCase().endsWith(".pdf");
        String rawContentType = file.getContentType();
        String contentType = rawContentType != null ? rawContentType.toLowerCase() : "";
        boolean contentTypePdf = contentType.contains("pdf");
        if (!extensionPdf && !contentTypePdf) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        Long safeUserId = Objects.requireNonNull(userId, "userId is required");
        User user = userRepository.findById(safeUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CandidateProfile profile = candidateProfileRepository.findByUserId(safeUserId).orElseGet(CandidateProfile::new);
        profile.setUser(user);
        if (profile.getSkills() == null) {
            profile.setSkills("");
        }

        try {
            Files.createDirectories(resumeDir);
            if (profile.getResumePath() != null && !profile.getResumePath().isBlank()) {
                try {
                    Files.deleteIfExists(Path.of(profile.getResumePath()));
                } catch (IOException ignored) {
                    // Non-fatal cleanup failure
                }
            }

            String storedName = "resume_user_" + userId + "_" + System.currentTimeMillis() + ".pdf";
            Path target = resumeDir.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            profile.setResumeFileName(originalName);
            profile.setResumePath(target.toString());
            profile.setResumeUploadedAt(LocalDateTime.now());

            List<String> detectedSkills = List.of();
            String parseStatus = "success";
            String parseMessage = "Resume parsed successfully";
            String inferredPreferredRoles = "";
            String detectedLocation = "";
            Integer parsedExperienceYears = null;
            String generatedBio = "";
            try (var in = Files.newInputStream(target)) {
                String text = ResumeParserUtil.extractTextFromPdf(in);
                if (text == null || text.isBlank()) {
                    parseMessage =
                            "Resume uploaded; no selectable text was found in this PDF (often true for scanned images). Add skills manually or export a text-based PDF.";
                    detectedSkills = List.of();
                } else {
                    detectedSkills = ResumeParserUtil.extractSkills(text);
                    inferredPreferredRoles = ResumeProfileInference.inferPreferredRole(detectedSkills);
                    detectedLocation = ResumeProfileInference.detectLocation(text);
                    parsedExperienceYears = ResumeProfileInference.detectExperienceYears(text);
                    generatedBio = ResumeProfileInference.generateBio(text, detectedSkills);
                    applyParsedProfileFields(profile, inferredPreferredRoles, detectedLocation, parsedExperienceYears, generatedBio);
                    if (detectedSkills.isEmpty()) {
                        parseMessage =
                                "Resume uploaded; text was read but no known technical skills were detected. Add skills manually or use clearer skill keywords.";
                    }
                }
            } catch (Exception ex) {
                // Skill extraction is best-effort; resume upload should still succeed.
                parseStatus = "warning";
                parseMessage = "Resume uploaded, but text extraction failed. You can still add skills manually.";
                detectedSkills = List.of();
                inferredPreferredRoles = "";
                detectedLocation = "";
                parsedExperienceYears = null;
                generatedBio = "";
            }

            profile.setExtractedSkills(String.join(",", detectedSkills));
            if (!detectedSkills.isEmpty()) {
                profile.setSkills(mergeCommaSeparated(profile.getSkills(), detectedSkills));
            }

            candidateProfileRepository.saveAndFlush(profile);

            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("message", "Resume uploaded successfully");
            out.put("fileName", profile.getResumeFileName());
            out.put("resumeFileName", resolveResumeFileName(profile));
            out.put("resumeUploadedAt", profile.getResumeUploadedAt());
            out.put("resumeAvailable", true);
            out.put("downloadUrl", "/api/profile/candidate/" + userId + "/resume");
            out.put("detectedSkills", detectedSkills);
            out.put("mergedSkills", formatSkillsForResponse(profile.getSkills()));
            out.put("extractedSkills", profile.getExtractedSkills() != null ? profile.getExtractedSkills() : "");
            out.put("preferredRoles", profile.getPreferredRoles() != null ? profile.getPreferredRoles() : "");
            out.put("location", profile.getLocation() != null ? profile.getLocation() : "");
            out.put("experienceYears", profile.getExperienceYears());
            out.put("bio", profile.getBio() != null ? profile.getBio() : "");
            out.put("parseStatus", parseStatus);
            out.put("parseMessage", parseMessage);
            return out;
        } catch (IOException ex) {
            throw new BadRequestException("Failed to upload resume. Please try again.");
        }
    }

    public Map<String, Object> getCandidateResumeMeta(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));
        if (profile.getResumePath() == null || profile.getResumePath().isBlank()) {
            throw new NotFoundException("Resume not uploaded yet");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("fileName", resolveResumeFileName(profile));
        out.put("resumePath", profile.getResumePath());
        out.put("uploadedAt", profile.getResumeUploadedAt());
        return out;
    }

    private static String resolveResumeFileName(CandidateProfile profile) {
        if (profile == null) {
            return null;
        }
        String explicit = profile.getResumeFileName();
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String resumePath = profile.getResumePath();
        if (resumePath == null || resumePath.isBlank()) {
            return null;
        }
        try {
            Path p = Path.of(resumePath);
            Path file = p.getFileName();
            if (file != null) {
                String name = file.toString();
                return name.isBlank() ? null : name;
            }
        } catch (Exception ignored) {
            // Fallback to null
        }
        return null;
    }

    private String mergeCommaSeparated(String existingCsv, List<String> add) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();

        for (String s : splitCsv(existingCsv)) {
            String k = normalizeSkillKey(s);
            if (k.isBlank() || seen.contains(k)) continue;
            seen.add(k);
            out.add(s.trim());
        }
        for (String s : add) {
            String k = normalizeSkillKey(s);
            if (k.isBlank() || seen.contains(k)) continue;
            seen.add(k);
            out.add(s.trim());
        }
        return String.join(",", out);
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        String[] parts = csv.split("[,;|]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.trim().isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }

    /** Human-readable skills list for API clients (e.g. resume upload JSON). DB still stores comma-separated. */
    private static String formatSkillsForResponse(String skillsCsv) {
        if (skillsCsv == null || skillsCsv.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String p : skillsCsv.split("[,;|]+")) {
            if (p != null && !p.trim().isBlank()) {
                parts.add(p.trim());
            }
        }
        return String.join(", ", parts);
    }

    private String normalizeSkillKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }

    /**
     * When PDF text parses successfully, apply inferred fields from the resume (non-blank values only).
     */
    private void applyParsedProfileFields(
            CandidateProfile profile,
            String preferredRoles,
            String location,
            Integer experienceYears,
            String bio) {
        if (preferredRoles != null && !preferredRoles.isBlank()) {
            profile.setPreferredRoles(preferredRoles.trim());
        }
        if (location != null && !location.isBlank()) {
            profile.setLocation(location.trim());
        }
        if (experienceYears != null) {
            profile.setExperienceYears(experienceYears);
        }
        if (bio != null && !bio.isBlank()) {
            profile.setBio(bio.trim());
        }
    }
}

