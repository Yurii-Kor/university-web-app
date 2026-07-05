# University Schedule

A minimal university **timetable** app for Students and Teachers with **Course** and **Group** viewes.



## Domain

- **Teacher** teaches one or more **Course**.

- Each **Course** has `0..\*` **Group** (study groups within a course).

- **Student** can belong to many **Group**, but **at most one group per course** (service rule).

- **ScheduleEntry** is a class slot for a **Group**: `start`, `end`, `room`.

- Main feature: **timetable** for month/day with optional filters by course/group.



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


