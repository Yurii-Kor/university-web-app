-- ============================================================
-- Teachers
-- ============================================================

insert into app_user (
    id,
    email,
    password,
    role,
    first_name,
    last_name,
    enabled
)
values
    (
        1101,
        'alice.teacher@example.com',
        'encoded-password',
        'TEACHER',
        'Alice',
        'Anderson',
        true
    ),
    (
        1102,
        'bob.teacher@example.com',
        'encoded-password',
        'TEACHER',
        'Bob',
        'Brown',
        true
    );


insert into teacher (
    id,
    academic_rank,
    office
)
values
    (
        1101,
        'LECTURER',
        'T-101'
    ),
    (
        1102,
        'LECTURER',
        'T-102'
    );


-- ============================================================
-- Groups
-- ============================================================

insert into groups (
    id,
    name
)
values
    (
        2001,
        'CS-101'
    ),
    (
        2002,
        'SE-202'
    ),
    (
        2003,
        'EE-303'
    );


-- ============================================================
-- Courses
--
-- Courses 3001 and 3002 currently belong to teacher 1101.
-- Course 3003 belongs to teacher 1102.
--
-- Lesson 4002 below intentionally keeps teacher 1102 while its
-- course currently belongs to teacher 1101. This verifies that
-- lesson queries use lessons.teacher_id / assignedTeacher rather
-- than courses.teacher_id.
-- ============================================================

insert into courses (
    id,
    code,
    name,
    description,
    teacher_id
)
values
    (
        3001,
        'CSE-JAVA',
        'Java Programming',
        'Java programming course',
        1101
    ),
    (
        3002,
        'CSE-DB',
        'Database Systems',
        'Database systems course',
        1101
    ),
    (
        3003,
        'NET-101',
        'Computer Networks',
        'Computer networks course',
        1102
    );


-- ============================================================
-- Course-to-group assignments
-- ============================================================

insert into group_courses (
    group_id,
    course_id
)
values
    (2001, 3001),
    (2001, 3002),
    (2002, 3001),
    (2002, 3003),
    (2003, 3003);


-- ============================================================
-- Lessons
--
-- 4001:
--   baseline lesson
--
-- 4002:
--   same group as 4001
--   course belongs to teacher 1101
--   assigned lesson teacher is deliberately 1102
--
-- 4003:
--   same assigned teacher as 4001
--
-- 4004:
--   room equals 4001 ignoring case
--
-- 4005 / 4006:
--   end/start exactly at overlap query boundaries
--
-- 4007:
--   future lesson for course 3001
--
-- 4008:
--   starts exactly at schedule upper bound / future cutoff
-- ============================================================

insert into lessons (
    id,
    group_id,
    course_id,
    teacher_id,
    start_time,
    end_time,
    room,
    lesson_type,
    description
)
values
    (
        4001,
        2001,
        3001,
        1101,
        '2030-01-10 10:00:00+00',
        '2030-01-10 11:00:00+00',
        'A-101',
        'LECTURE',
        'Core Java lecture'
    ),
    (
        4002,
        2001,
        3002,
        1102,
        '2030-01-10 10:30:00+00',
        '2030-01-10 11:30:00+00',
        'B-201',
        'PRACTICE',
        'Group overlap lesson'
    ),
    (
        4003,
        2002,
        3001,
        1101,
        '2030-01-10 10:15:00+00',
        '2030-01-10 10:45:00+00',
        'C-301',
        'LAB',
        'Teacher overlap lesson'
    ),
    (
        4004,
        2002,
        3003,
        1102,
        '2030-01-10 10:20:00+00',
        '2030-01-10 10:50:00+00',
        'a-101',
        'SEMINAR',
        'Room overlap lesson'
    ),
    (
        4005,
        2003,
        3003,
        1102,
        '2030-01-10 09:00:00+00',
        '2030-01-10 10:00:00+00',
        'D-401',
        'OTHER',
        'Adjacent lesson before window'
    ),
    (
        4006,
        2003,
        3003,
        1102,
        '2030-01-10 11:00:00+00',
        '2030-01-10 12:00:00+00',
        'D-402',
        'EXAM',
        'Adjacent lesson after window'
    ),
    (
        4007,
        2001,
        3001,
        1101,
        '2030-01-11 09:00:00+00',
        '2030-01-11 10:00:00+00',
        'E-501',
        'LECTURE',
        'Future Java lecture'
    ),
    (
        4008,
        2001,
        3001,
        1101,
        '2030-01-10 12:00:00+00',
        '2030-01-10 13:00:00+00',
        'E-502',
        'PRACTICE',
        'Starts exactly at upper bound'
    );