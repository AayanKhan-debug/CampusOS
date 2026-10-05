# Architecture Simple Flow

## High-Level Flow
1. **Client Request:** React Frontend (Vite) makes API requests via Axios.
2. **Security Filter:** Spring Security intercepts the request. The JwtAuthFilter extracts and validates the token.
3. **Controller Layer:** Receives the validated request, validates DTOs, and forwards to the Service layer.
4. **Service Layer:** Contains business logic (e.g., event waitlist promotion, checking club capacities, resolving help desk tickets).
5. **Data Access Layer:** Spring Data JPA executes Hibernate queries against the MySQL Database.
6. **Response:** DTO responses are mapped and sent back to the frontend, consumed by TanStack Query for caching and state management.

## Code Splitting (Frontend)
- Utilizes React.lazy and Suspense for route-level chunking.
- Rollup manualChunks separately bundles React and Query dependencies.
- Keeps initial load size < 100KB.
