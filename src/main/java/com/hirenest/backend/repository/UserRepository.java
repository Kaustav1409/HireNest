package com.hirenest.backend.repository;

import com.hirenest.backend.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    /**
     * Returns only email strings without loading full User entities or any
     * lazily-fetched relationships. Used by the debug-users diagnostic endpoint.
     */
    @Query("SELECT u.email FROM User u")
    List<String> findAllEmails();
}
