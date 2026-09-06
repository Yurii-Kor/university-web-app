insert into groups (
	id,
	name
)
values
	(2001, 'CS-101'),
	(2002, 'EE-205');

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
	),
	(
		1103,
		'teacher.three@example.com',
		'encoded-password',
		'TEACHER',
		'David',
		'Clark',
		true
	),
	(
		1104,
		'blocked.teacher@example.com',
		'encoded-password',
		'TEACHER',
		'Blocked',
		'Teacher',
		false
	),
	(
		1105,
		'deleted.teacher@example.com',
		'encoded-password',
		'TEACHER',
		'Deleted',
		'Teacher',
		true
	),
	(
		1201,
		'student.cs@example.com',
		'encoded-password',
		'STUDENT',
		'John',
		'Student',
		true
	),
	(
		1202,
		'student.ee@example.com',
		'encoded-password',
		'STUDENT',
		'Emma',
		'Student',
		true
	);

insert into teacher (
	id,
	academic_rank,
	office,
	deleted_at
)
values
	(1101, 'LECTURER', 'A-101', null),
	(1102, 'PROFESSOR', 'B-202', null),
	(1103, 'LECTURER', 'C-303', null),
	(1104, 'LECTURER', 'D-404', null),
	(1105, 'PROFESSOR', 'E-505', now());

insert into student (
	id,
	group_id,
	enrollment_year,
	deleted_at
)
values
	(1201, 2001, 2024, null),
	(1202, 2002, 2024, null);

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
		'EE-CIRCUITS',
		'Electronic Circuits',
		'Electronic circuits course',
		1102
	),
	(
		3004,
		'CS-BLOCKED',
		'Blocked Teacher Course',
		'Course assigned to disabled teacher',
		1104
	);

insert into group_courses (
	group_id,
	course_id
)
values
	(2001, 3001),
	(2001, 3002),
	(2002, 3003),
	(2001, 3004);