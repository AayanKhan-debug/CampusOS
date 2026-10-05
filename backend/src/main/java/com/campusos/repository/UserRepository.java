package com.campusos.repository;

import com.campusos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository — data access layer for the User entity.
 *
 * Extends JpaRepository which gives us free CRUD methods:
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * We only need to declare methods with non-standard queries.
 * Spring Data JPA derives the SQL automatically from the method name.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used during login to look up the account.
     *
     * Spring Data generates: SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if an email is already registered.
     * Used during registration to prevent duplicates.
     *
     * Spring Data generates: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * Returns a boolean — more efficient than loading the whole entity.
     */
    boolean existsByEmail(String email);
}
