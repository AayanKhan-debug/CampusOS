package com.campusos.config;

import com.campusos.entity.User;
import com.campusos.enums.Role;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataInitializer — seeds default demo/admin accounts on startup if not present.
 * Ensures that ADMIN and CLUB_ADMIN accounts are available for testing and evaluation.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default ADMIN if not exists
        if (!userRepository.existsByEmail("admin@campus.edu")) {
            User admin = User.builder()
                    .name("Campus Administrator")
                    .email("admin@campus.edu")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .department("Administration")
                    .build();
            userRepository.save(admin);
            log.info("Seeded default ADMIN user: admin@campus.edu");
        }

        // Seed or update CLUB_ADMIN if exists
        userRepository.findByEmail("leader@campus.edu").ifPresentOrElse(
                user -> {
                    if (user.getRole() != Role.CLUB_ADMIN) {
                        user.setRole(Role.CLUB_ADMIN);
                        userRepository.save(user);
                        log.info("Updated leader@campus.edu role to CLUB_ADMIN");
                    }
                },
                () -> {
                    User leader = User.builder()
                            .name("Club Leader")
                            .email("leader@campus.edu")
                            .password(passwordEncoder.encode("password123"))
                            .role(Role.CLUB_ADMIN)
                            .department("Computer Science")
                            .year(3)
                            .build();
                    userRepository.save(leader);
                    log.info("Seeded default CLUB_ADMIN user: leader@campus.edu");
                }
        );
    }
}
