package com.hirenest.backend.seed;

import com.hirenest.backend.entity.Job;
import com.hirenest.backend.entity.User;
import com.hirenest.backend.repository.JobRepository;
import com.hirenest.backend.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class PlatformJobSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformJobSeeder.class);

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformJobSeeder(
            JobRepository jobRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (jobRepository.countByPlatformJobTrue() > 0) {
            log.info("Platform jobs already seeded (count={})", jobRepository.countByPlatformJobTrue());
            return;
        }

        User platformUser = userRepository.findByEmail(PlatformJobCatalog.PLATFORM_USER_EMAIL)
                .orElseGet(this::createPlatformUser);

        List<PlatformJobSeed> seeds = PlatformJobCatalog.build();
        int inserted = 0;
        for (PlatformJobSeed seed : seeds) {
            if (jobRepository.existsBySourceKey(seed.sourceKey)) {
                continue;
            }
            Job job = new Job();
            job.setRecruiter(platformUser);
            job.setTitle(seed.title);
            job.setCompanyName(seed.companyName);
            job.setDescription(seed.description);
            job.setRequiredSkills(seed.requiredSkills);
            job.setLocation(seed.location);
            job.setRemote(seed.remote);
            job.setSalaryMin(seed.salaryMin);
            job.setSalaryMax(seed.salaryMax);
            job.setExperienceLevel(seed.experienceLevel);
            job.setCategory(seed.category);
            job.setRoleType(seed.roleType);
            job.setPlatformJob(true);
            job.setSourceKey(seed.sourceKey);
            jobRepository.save(job);
            inserted++;
        }
        log.info("Seeded {} HireNest Partner Network jobs", inserted);
    }

    private User createPlatformUser() {
        User user = new User();
        user.setFullName("HireNest Partner Network");
        user.setEmail(PlatformJobCatalog.PLATFORM_USER_EMAIL);
        user.setPassword(passwordEncoder.encode("PlatformSeed!2026"));
        user.setRole("RECRUITER");
        user.setAuthProvider("LOCAL");
        return userRepository.save(user);
    }
}
