-- ============================================================
-- Groups
-- ============================================================

insert into groups (id, name)
values
	(2001, 'CS-101'),
	(2002, 'SE-205'),
	(2003, 'EE-205'),
	(2004, 'ME-101');


-- ============================================================
-- Student users
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
		1001,
		'john.smith@example.com',
		'encoded-password',
		'STUDENT',
		'John',
		'Smith',
		true
	),
	(
		1002,
		'alice.brown@example.com',
		'encoded-password',
		'STUDENT',
		'Alice',
		'Brown',
		true
	),
	(
		1003,
		'blocked.student@example.com',
		'encoded-password',
		'STUDENT',
		'Blocked',
		'Student',
		false
	),
	(
		1004,
		'deleted.student@example.com',
		'encoded-password',
		'STUDENT',
		'Deleted',
		'Student',
		true
	),
	(
		1005,
		'david.wilson@example.com',
		'encoded-password',
		'STUDENT',
		'David',
		'Wilson',
		true
	),
	(
		1006,
		'emma.davis@example.com',
		'encoded-password',
		'STUDENT',
		'Emma',
		'Davis',
		true
	);


-- ============================================================
-- Students
--
-- CS-101:
--   1001 active/enabled
--   1002 active/enabled
--   1003 active/disabled
--   1004 soft-deleted
--
-- SE-205:
--   1005 active/enabled
--
-- EE-205:
--   1006 active/enabled
-- ============================================================

insert into student (
	id,
	group_id,
	enrollment_year,
	deleted_at
)
values
	(1001, 2001, 2024, null),
	(1002, 2001, 2024, null),
	(1003, 2001, 2025, null),
	(1004, 2001, 2023, now()),
	(1005, 2002, 2025, null),
	(1006, 2003, 2024, null);


-- ============================================================
-- Teacher users
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
		'teacher.one@example.com',
		'encoded-password',
		'TEACHER',
		'Robert',
		'Miller',
		true
	),
	(
		1102,
		'teacher.two@example.com',
		'encoded-password',
		'TEACHER',
		'Sarah',
		'Anderson',
		true
	);


-- ============================================================
-- Teachers
-- ============================================================

insert into teacher (
	id,
	academic_rank,
	office
)
values
	(1101, 'LECTURER', 'A-101'),
	(1102, 'LECTURER', 'B-202');


-- ============================================================
-- Courses
--
-- Teacher 1 deliberately has two courses in CS-101.
-- Teacher card query must still return each student once.
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
		'CS-JAVA',
		'Java Programming',
		'Java programming course',
		1101
	),
	(
		3002,
		'CS-SPRING',
		'Spring Framework',
		'Spring Framework course',
		1101
	),
	(
		3003,
		'SE-DB',
		'Database Systems',
		'Database systems course',
		1101
	),
	(
		3004,
		'EE-CIRCUITS',
		'Electronic Circuits',
		'Electronic circuits course',
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
	(2002, 3003),
	(2003, 3004);