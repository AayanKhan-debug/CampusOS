package com.campusos.security;

import com.campusos.entity.User;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CustomUserDetailsService — bridges our User entity with Spring Security.
 *
 * Spring Security needs a UserDetailsService to load user information during
 * authentication. It calls loadUserByUsername() with the username (email for us).
 *
 * We implement this interface and tell Spring Security to use OUR implementation
 * instead of the default in-memory one.
 *
 * The returned UserDetails object contains:
 * - username (email)
 * - password (BCrypt hash) — Spring Security uses this to verify login attempts
 * - authorities (roles) — Spring Security uses these for authorization checks
 */
@Service
@RequiredArgsConstructor  // Lombok: generates constructor for all @final fields
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find the user in the database. If not found, throw the exception
        // Spring Security expects — it converts this into a 401 response.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with email: " + email
                ));

        /*
         * Convert our User entity into Spring Security's UserDetails.
         *
         * We prefix role names with "ROLE_" because Spring Security's
         * hasRole("STUDENT") internally checks for "ROLE_STUDENT".
         * Using hasAuthority("ROLE_STUDENT") also works without the prefix trick.
         *
         * Example: Role.STUDENT → "ROLE_STUDENT"
         */
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
