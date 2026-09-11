truncate table
	lessons,
	group_courses,
	courses,
	student,
	teacher,
	groups,
	app_user
restart identity cascade;

insert into groups (
	id,
	name
) values
	(2001, 'CS-101'),
	(2002, 'CS-102'),
	(2003, 'CS-103');

insert into app_user (
	id,
	email,
	password,
	role,
	first_name,
	last_name,
	enabled
) values
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

insert into teacher (
	id,
	academic_rank,
	office
) values
	(1101, 'LECTURER', 'A-101'),
	(1102, 'PROFESSOR', 'B-202');

insert into courses (
	id,
	code,
	name,
	description,
	teacher_id
) values
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
		'CS-DB',
		'Database Systems',
		'Database systems course',
		1102
	);

insert into group_courses (
	group_id,
	course_id
) values
	(2001, 3001),
	(2001, 3003),
	(2002, 3002),
	(2003, 3003);

-- Group conflict:
-- another assigned teacher and room, same group.
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
) values (
	4001,
	2001,
	3003,
	1102,
	'2030-01-10 10:00:00+00',
	'2030-01-10 11:00:00+00',
	'G-101',
	'LECTURE',
	'Existing group lesson'
);

-- Teacher conflict:
-- same assigned teacher, another group and room.
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
) values (
	4002,
	2002,
	3002,
	1101,
	'2030-01-10 12:00:00+00',
	'2030-01-10 13:00:00+00',
	'T-202',
	'PRACTICE',
	'Existing teacher lesson'
);

-- Room conflict:
-- another assigned teacher and group, same room.
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
) values (
	4003,
	2003,
	3003,
	1102,
	'2030-01-10 14:00:00+00',
	'2030-01-10 15:00:00+00',
	'R-303',
	'LAB',
	'Existing room lesson'
);

-- Main lesson used by update and delete tests.
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
) values (
	4100,
	2002,
	3002,
	1101,
	'2030-01-11 16:00:00+00',
	'2030-01-11 17:00:00+00',
	'U-404',
	'SEMINAR',
	'Update target'
);

-- Lesson already in progress at TEST_NOW = 2030-01-10 09:00 UTC.
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
) values (
	4101,
	2001,
	3001,
	1101,
	'2030-01-10 08:30:00+00',
	'2030-01-10 09:30:00+00',
	'S-101',
	'LECTURE',
	'Started lesson'
);

-- Deliberately decoupled assignment:
-- course 3001 currently belongs to teacher 1101,
-- but this particular lesson is assigned to teacher 1102.
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
) values (
	4102,
	2001,
	3001,
	1102,
	'2030-01-11 20:00:00+00',
	'2030-01-11 21:00:00+00',
	'X-606',
	'LECTURE',
	'Lesson assigned to another teacher'
);