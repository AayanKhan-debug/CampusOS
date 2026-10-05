# CampusOS — Student Platform Frontend (Phase 6)

A modern, student-centric campus platform frontend built with **React**, **Vite**, **Tailwind CSS**, and **TanStack Query**. Built to interface directly with the production Spring Boot 3.x backend (`http://localhost:8080`).

---

## 🚀 Key Features & Modules

### 1. Authentication & Security
- **JWT Authentication**: Token persistence in `localStorage`, automatic Bearer header injection via Axios interceptor, and seamless automatic redirect on 401 Unauthorized.
- **Login with Demo Quick-Fill**: Pre-configured buttons to instantly test as a **Student**, **Club Admin**, or **Campus Administrator**.
- **Student Registration**: Validated form with academic metadata (Department, Academic Year, Roll Number, Phone Number) backed by **Zod** and **React Hook Form**.

### 2. Student Dashboard
- **Live Campus Snapshot**: Greeting banner with academic year, live counters for registered events, unread announcements, active help desk tickets, and opportunities.
- **Registered Events Widget**: Tracks upcoming confirmed and waitlisted events.
- **Notice Board Feed**: Unread announcements with instant mark-as-read action.
- **Quick Actions**: One-click shortcuts to explore events, discover clubs, access notes, and submit help desk tickets.

### 3. Events & Registration
- **Event Discovery**: Filter by categories (`TECHNICAL`, `CULTURAL`, `SPORTS`, `WORKSHOP`, `SEMINAR`, `HACKATHON`, `COMPETITION`, `CLUB`, `OTHER`) and statuses (`UPCOMING`, `ONGOING`, `COMPLETED`, `CANCELLED`).
- **Capacity Management**: Visual progress bar tracking registered students against event capacity.
- **Waitlist Support**: Automatically prompts students to join the waitlist when events reach maximum capacity.
- **Registration Cancellation**: Students can view and cancel their registrations with spot-release confirmation.

### 4. Clubs & Student Chapters
- **Club Directory**: Browse student organizations by category with active member rosters and lead student details.
- **Membership Application**: Join request workflow (`POST /api/clubs/{clubId}/join`).
- **Member Roster Modal**: Inspect active club officers and members.

### 5. Announcements & Circulars
- **Official Notice Board**: Categorized notices (`ACADEMIC`, `EXAM`, `PLACEMENT`, `EVENT`, `CLUB`, `GENERAL`).
- **Pinned Highlights**: Pinned circulars prominently accented with pinned tags.
- **Read Tracking**: Unread tab dynamically tracks notices yet to be read, with instant mark-as-read integration (`POST /api/announcements/{id}/read`).
- **Reader Dialog**: Distraction-free full-text reader with image attachments and timestamps.

### 6. Academic Resource Hub
- **Hierarchical Drill-Down**: Navigate academic materials cleanly: `Department` → `Course / Degree` → `Subject` → `Resources`.
- **Search & Filter**: Direct keyword search and filtering across notes, past question papers, lab manuals, assignments, syllabus, and video links.
- **Direct Resource Access**: One-click access to cloud-stored documents and resources.

### 7. Lost & Found Portal
- **Item Listings**: Dual feed for `LOST` and `FOUND` campus items with search and status filters.
- **Report Item Modal**: Form with location, date occurred, category, contact information, and optional image URL.
- **Status State Machine**: Listing owners and administrators can transition listings through valid lifecycles (`OPEN` → `MATCHED` → `RECOVERED` → `CLOSED`).

### 8. Career & Student Opportunities
- **Curated Opportunities**: Hackathons, internships, scholarships, coding contests, and placement drives.
- **Remote Filter**: Quick toggle for remote vs. on-campus opportunities.
- **Organizer Posting**: Role-gated opportunity creation modal for `CLUB_ADMIN` and `ADMIN` users.

### 9. Campus Help Desk
- **Maintenance & Facilities Reporting**: Report issues across `INTERNET`, `ELECTRICAL`, `HOSTEL`, `CLASSROOM`, `INFRASTRUCTURE`, `CLEANLINESS`, and `WATER`.
- **Priority Tracking**: Priority classification (`LOW`, `MEDIUM`, `HIGH`, `URGENT`).
- **Lifecycle Management**: Students can view assigned staff, edit/withdraw pending submissions, and confirm resolution (`PATCH status=CLOSED`).

### 10. Student Profile & Portfolio
- **Academic Credentials**: Department, year, roll number, and bio.
- **Skill & Interest Chips**: Display and edit technical skills and campus interests.
- **Inline Editor**: Synchronized with `PUT /api/users/me` and the application's auth state.

---

## 🎨 Design System & UX Standards

- **Linear / Notion Aesthetic**: Minimalist borders (`border-border/60`), muted surfaces, refined typography, and subtle micro-interactions.
- **Light & Dark Mode**: Persistent theme toggle via `ThemeContext` using standard CSS classes and HTML attributes.
- **Command Palette (`Ctrl + K` / `Cmd + K`)**: Global spotlight search allowing students to jump to any page, switch themes, or sign out via keyboard.
- **Responsive Layout**:
  - **Desktop**: Persistent modern sidebar navigation with user pill.
  - **Mobile**: Ergonomic bottom navigation bar with quick touch targets.
  - **Tablet**: Adaptive layout with clean header bar.

---

## 🛠️ Tech Stack

- **Runtime & Build**: React 18, Vite
- **Styling**: Tailwind CSS, PostCSS, Autoprefixer
- **State & Data Fetching**: TanStack Query (React Query)
- **Routing**: React Router v6
- **Forms & Validation**: React Hook Form, Zod, `@hookform/resolvers`
- **UI Primitives**: Radix UI (`@radix-ui/react-dialog`), Lucide Icons
- **Notifications**: Sonner

---

## 💻 Getting Started

### 1. Prerequisites
- **Node.js**: v18+ (tested on Node v24)
- **Backend**: CampusOS Spring Boot server running on `http://localhost:8080`

### 2. Configuration
The frontend uses `.env` for the API URL:
```bash
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Installation & Run
```bash
# Navigate to frontend
cd frontend

# Install dependencies (already pre-installed)
npm install

# Start Vite development server
npm run dev
```

### 4. Production Build & Verification
```bash
npm run build
npm run preview
```

---

## 🔑 Demo Accounts

Use these pre-seeded accounts or the quick-fill buttons on the `/login` page:

| Role | Email | Password |
| :--- | :--- | :--- |
| **Student** | `student1@campus.edu` | `password123` |
| **Club Admin** | `admin.coding@campus.edu` | `password123` |
| **Admin** | `admin@campus.edu` | `password123` |
