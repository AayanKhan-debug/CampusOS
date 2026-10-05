# CampusOS

CampusOS is a full-stack student-centric platform designed to manage campus activities, clubs, events, resources, lost & found, and a campus help desk. Built as a serious resume/placement project.

## Features
- **Phase 1: Authentication & Users** - JWT-based stateless auth, role-based access control (STUDENT, CLUB_LEADER, ADMIN).
- **Phase 2: Clubs & Memberships** - Club creation, approval workflows, membership statuses.
- **Phase 3: Events & Event Registration** - Event creation, capacity limits, automatic waitlist promotion, cancellations.
- **Phase 4: Announcements & Resource Hub** - Global/Club announcements, hierarchical resources (Dept > Course > Subject > PDF/Links).
- **Phase 5: Lost & Found, Opportunities, Help Desk** - Item reporting/matching, job/internship listings, issue tracking/ticketing.

## Tech Stack
- **Backend:** Java 21, Spring Boot 3, Spring Data JPA (Hibernate), Spring Security (JWT), MySQL.
- **Frontend:** React 18, Vite, React Router (Code splitting with Suspense/Lazy), Tailwind CSS v4, shadcn/radix-ui, TanStack Query.
- **Testing:** JUnit 5, Mockito. (113+ Backend Unit Tests).

## Setup & Deployment

### Local Development
1. Clone the repository and ensure MySQL is running on localhost:3306.
2. Backend: Set environment variables in ackend/.env (use ackend/.env.example).
   Run: cd backend && mvn spring-boot:run
3. Frontend: Set variables in rontend/.env (use rontend/.env.example).
   Run: cd frontend && npm install && npm run dev

### Production Deployment
**Backend:**
- Compile: mvn clean package -DskipTests
- Run the JAR: java -jar target/campus-os-0.0.1-SNAPSHOT.jar
- Alternatively, use the provided Dockerfile.

**Frontend:**
- Build: 
pm run build
- Deploy the dist folder to Nginx or a static host (Vercel, Netlify).
- Ensure VITE_API_BASE_URL points to the production backend URL during build.

## API Documentation
Once the backend is running, access Swagger UI at:
- http://localhost:8080/swagger-ui.html

