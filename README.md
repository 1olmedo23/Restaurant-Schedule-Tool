## **Restaurant Schedule tool**

---
A mobile-friendly restaurant scheduling tool built with **Spring Boot 3 + Thymeleaf + Bootstrap + PostgreSQL**.  
Managers can build schedules using employee availability (Tue–Sat, lunch/dinner), override when needed, and publish a two-week calendar. Employees see their personal two-week schedule.

--- 

## Tech Stack
- Java 17+ (works on 21 as well)
- Spring Boot 3.3.x (Web, Security, Data JPA, Thymeleaf)
- PostgreSQL
- Maven
- Bootstrap

---

## Key Features
- **Auth & Roles**: `MANAGER`, `EMPLOYEE` (DB-backed).
- **Admin**: create users (choose one role), enable/disable with confirmation, hard delete (deletes availability & assignments).
- **Availability**: Tue–Sat with lunch/dinner toggles; persists visually on save.
- **Scheduling (Manager)**:
    - 2-week schedule builder starting today.
    - Day view with Lunch/Dinner sections and role selects filtered by availability.
    - **Manager role restricted** to MANAGERs.
    - Per-role **Override availability** to show all staff (or managers-only).
    - Save & Clear-All with flash banners.
- **My Schedule**: two-week grid (static shape). Assigned roles populate cells; Sundays/Mondays shaded “closed”.

  
// Optional Postgres profile: -Dspring-boot.run.profiles=postgres
