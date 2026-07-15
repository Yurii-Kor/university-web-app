# University Timetable Management System

### GitHub Actions · Continuous Integration · Docker Publishing · Project Versioning · Python Scripts

[![University Web App CI](https://github.com/Yurii-Kor/university-web-app/actions/workflows/university-web-app-ci.yml/badge.svg)](https://github.com/Yurii-Kor/university-web-app/actions/workflows/university-web-app-ci.yml)
[![Publish Docker](https://github.com/Yurii-Kor/university-web-app/actions/workflows/publish-docker.yml/badge.svg)](https://github.com/Yurii-Kor/university-web-app/actions/workflows/publish-docker.yml)
[![Update Project Version](https://github.com/Yurii-Kor/university-web-app/actions/workflows/update-project-version.yml/badge.svg)](https://github.com/Yurii-Kor/university-web-app/actions/workflows/update-project-version.yml)
![Python Scripts](https://img.shields.io/badge/Scripts-Python-3776AB?logo=python&logoColor=white)

### Java 21 · Spring Boot 3.5.5 · Spring MVC · Spring Security · Thymeleaf · Spring Data JPA · PostgreSQL · HTML5 · CSS3

![Java 21](https://img.shields.io/badge/Java-21-E76F00?logo=openjdk&logoColor=white)
![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-6DB33F?logo=springboot&logoColor=white)
![Spring MVC](https://img.shields.io/badge/Web-Spring%20MVC-6DB33F?logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Security-Spring%20Security-6DB33F?logo=springsecurity&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Views-Thymeleaf-005F0F?logo=thymeleaf&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Persistence-Spring%20Data%20JPA-59666C?logo=hibernate&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![HTML5](https://img.shields.io/badge/Markup-HTML5-E34F26?logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/Styles-CSS3-1572B6?logo=css&logoColor=white)
![Docker](https://img.shields.io/badge/Containers-Docker-2496ED?logo=docker&logoColor=white)
![Application](https://img.shields.io/badge/Type-Web%20Application-6C757D)


A role-based university timetable and academic management web application built with [Spring Boot](https://www.baeldung.com/spring-boot-start), Spring MVC, Thymeleaf, Spring Data JPA, and PostgreSQL.

The application provides role-specific workflows for students, teachers, and administrators. It supports account and profile management, study groups, courses, scheduled lessons, timetable views, user role transitions, and soft deletion with restoration of supported records.

Authentication is implemented through Spring Security [form login](https://www.baeldung.com/spring-security-login). Authorization is enforced through both HTTP security configuration and [method-level security](https://www.baeldung.com/spring-security-method-security), while [Spring Security integration with Thymeleaf](https://www.baeldung.com/spring-security-thymeleaf) controls role-dependent navigation, available actions, and page content.

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


