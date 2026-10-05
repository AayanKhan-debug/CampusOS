# Interview Prep: 16 Architectural Questions

1. **Why Spring Boot for this project?** Highly structured, mature ecosystem, easy security integration (Spring Security), and JPA makes complex data models manageable.
2. **Why JWT for Authentication?** Stateless. It allows the server to scale without session replication, which is standard for modern SPAs.
3. **How does the Waitlist promotion work?** Wrapped in a @Transactional boundary. When a user cancels, the system queries the next WAITLISTED member ordered by egisteredAt and updates their status to CONFIRMED.
4. **How did you handle N+1 query problems in JPA?** Used DTO projections and JOIN FETCH where necessary to avoid multiple select queries for relationships.
5. **Why React + Vite instead of Next.js?** Since SEO is not critical for an internal dashboard, a pure SPA was lighter and easier to statically deploy.
6. **How did you optimize the frontend bundle?** Used React.lazy() for route-level splitting and Rollup's manualChunks to separate vendor libraries, reducing the main bundle to ~97KB.
7. **Why TanStack Query?** It manages server state separately from UI state, handles caching, retries, and background refetching perfectly.
8. **How did you secure endpoints?** SecurityFilterChain blocking non-public URLs, and @PreAuthorize for fine-grained role checks (e.g., ADMIN only for deleting events).
9. **How is DB data modeled?** Normalized MySQL schema using Spring Data JPA. E.g., ClubMember as a bridge table between User and Club containing extra metadata like joinedAt and status.
10. **Why Tailwind v4?** Zero-configuration JIT, no need for PostCSS/Autoprefixer boilerplate, very fast Vite compilation with @tailwindcss/vite.
11. **How do you handle errors globally?** Backend: @RestControllerAdvice mapping exceptions (e.g., ResourceNotFoundException) to standard ErrorResponse DTOs with proper HTTP status codes.
12. **How did you test the application?** 113 Unit/Integration Tests using JUnit 5, Mockito. Spring Boot Test for endpoint validation.
13. **How are secrets managed?** Removed from codebase. Loaded dynamically using Spring @Value and .env files.
14. **Why Dockerize?** Ensures environmental consistency. Separate multistage builds for Maven/Java and Node/Nginx.
15. **What was the hardest feature?** Event capacity + Waitlist logic. Needed careful synchronization/transactions to prevent overbooking.
16. **How does CORS work in this setup?** Spring Security is configured with a CorsConfigurationSource to allow specific origins (e.g., frontend host) to send credentials and methods like PATCH/DELETE.
