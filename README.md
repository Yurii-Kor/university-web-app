# University Timetable Management System

### Java 21 · Spring Boot 3.5.5 · Spring MVC · Spring Security · Thymeleaf · Spring Data JPA

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)
![Spring MVC](https://img.shields.io/badge/Web-Spring%20MVC-brightgreen)
![Spring Security](https://img.shields.io/badge/Security-Spring%20Security-brightgreen)
![Thymeleaf](https://img.shields.io/badge/Views-Thymeleaf-green)
![Spring Data JPA](https://img.shields.io/badge/Persistence-Spring%20Data%20JPA-blue)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
![Application](https://img.shields.io/badge/Type-Web%20Application-lightgrey)

A role-based university timetable and academic management web application built with [Spring Boot](https://www.baeldung.com/spring-boot-start), Spring MVC, Thymeleaf, Spring Data JPA, and PostgreSQL.

The application provides dedicated workflows for students, teachers, and administrators. It supports user and profile management, study groups, courses, scheduled lessons, timetable views, account role transitions, and soft deletion with restoration of supported records.

Authentication is implemented through Spring Security [form login](https://www.baeldung.com/spring-security-login). Authorization is enforced both at the HTTP configuration level and through [method-level security](https://www.baeldung.com/spring-security-method-security), while [Spring Security integration with Thymeleaf](https://www.baeldung.com/spring-security-thymeleaf) controls role-dependent navigation and page content.

The project follows a layered package structure that separates the domain model, Spring Data JPA persistence, application services, web presentation, and security concerns. Its test suite combines unit and integration testing with focused Spring MVC tests using [`@WebMvcTest`](https://www.baeldung.com/spring-boot-testing#unit-testing-with-webmvctest).


---



## User Stories



### Auth

**Story:** As a User (Student/Teacher), I log in with email + password and land on my own data.  

**Acceptance**

- **Given** valid credentials  

- **When** I submit the login  

- **Then** I receive my profile `{id, role, firstName, lastName}` and the UI shows my role badge  

- **Else** invalid credentials → clear error without extra details



---



### Courses (tile view + course details)



#### Student

**Story:** See the courses I study and open a course.  

**Acceptance**

- **Given** I’m logged in as Student  

- **When** I open *Courses*  

- **Then** I see tiles `{id, code, name}` of **my** courses  

- **And** clicking a tile opens **Course details** `{name, description, my group}`



#### Teacher

**Story:** See the courses I teach and open a course.  

**Acceptance**

- **Given** I’m logged in as Teacher  

- **When** I open *Courses*  

- **Then** I see tiles of **my** courses  

- **And** clicking a tile opens **Course details** `{name, description, groups of the current course}`  

- *(Optional later: edit name/description)*



---



### Groups (read-only in MVP)

#### Teacher

**Story:** For a selected course, see all its groups and each group’s roster.  

**Acceptance**

- **Given** the course belongs to me  

- **When** I open *Course details*  

- **Then** I see the list of groups `{id, title}`  

- **And** opening a group shows its students `{id, fullName, email}`  

- **And** no add/remove of students in MVP (pre-seeded data)



#### Student

**Story:** In a selected course, see my group and its roster.  

**Acceptance**

- **Given** I open *Course details*  

- **Then** I see exactly one **my group** for that course (if any)  

- **And** opening it shows the roster (read-only)



---



### Schedule (Month / Day)

#### Student

**Story:** View my timetable for a month and drill down to a day.  

**Acceptance**

- **Given** I’m logged in as Student  

- **When** I open *Schedule* for `YYYY-MM`  

- **Then** I see Month view with indicators on days that have classes for my groups  

- **And** clicking a day shows a sorted list by `start` with `{time, courseName, groupTitle, room}`  

- **And** optional filters by `course`  

- **TZ:** results respect local timezone (e.g. Asia/Jerusalem); DB stores UTC



#### Teacher

**Story:** View my timetable and manage group slots.  

**Acceptance (view)**

- Month/Day like Student, but for my courses/groups; filters by `course/group`

**Acceptance (CRUD)**

- **Given** I’m logged in as Teacher - manager of the *Schedule* according to my   

- **When** I create/update/delete a slot `{start, end, room}`  

- **Then** students in that group see the changes  

- **And** validation: `end > start`, **no overlaps within the same group**  

- **Else** not owner → forbidden



---



## Non-functional (short)

- **Timezones:** store timestamps in UTC; return ISO-8601; render in **Asia/Jerusalem**.

- **Ranges:** Month = `\[firstDayT00:00, firstDayNextMonthT00:00)`, Day = `\[dateT00:00, nextDateT00:00)`, in local TZ.

- **Sorting:** by `start` (then `end`, then `courseName`).

- **Access:** Students see only their groups; Teachers only their own courses/groups.



---



## Repo structure (docs)

- `docs/uml/uml-diagram.png`

- *(This README)*


