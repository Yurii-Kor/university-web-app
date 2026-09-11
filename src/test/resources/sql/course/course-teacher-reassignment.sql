insert into courses (
	id,
	code,
	name,
	description,
	teacher_id
) values (
	3003,
	'NET-404',
	'Computer Networks',
	'Target teacher course',
	1002
);

insert into group_courses (
	group_id,
	course_id
) values (
	2002,
	3003
);

-- Past lesson of Operating Systems.
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
	5001,
	2001,
	3001,
	1001,
	'2030-01-09 10:00:00+00',
	'2030-01-09 11:00:00+00',
	'C-101',
	'LECTURE',
	'Past OS lesson'
);

-- Ongoing lesson at TEST_NOW = 2030-01-10 09:00 UTC.
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
	5002,
	2001,
	3001,
	1001,
	'2030-01-10 08:30:00+00',
	'2030-01-10 09:30:00+00',
	'C-102',
	'LECTURE',
	'Ongoing OS lesson'
);

-- Future OS lessons without conflicts for teacher 1002.
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
) values
	(
		5003,
		2001,
		3001,
		1001,
		'2030-01-11 10:00:00+00',
		'2030-01-11 11:00:00+00',
		'C-103',
		'PRACTICE',
		'Future OS lesson one'
	),
	(
		5004,
		2002,
		3001,
		1001,
		'2030-01-11 13:00:00+00',
		'2030-01-11 14:00:00+00',
		'C-104',
		'LAB',
		'Future OS lesson two'
	);

-- Future Security lessons that would be reassigned to teacher 1002.
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
) values
	(
		5101,
		2001,
		3002,
		1001,
		'2030-01-10 10:00:00+00',
		'2030-01-10 11:00:00+00',
		'S-201',
		'LECTURE',
		'Conflicting Security lesson one'
	),
	(
		5102,
		2001,
		3002,
		1001,
		'2030-01-10 14:00:00+00',
		'2030-01-10 15:00:00+00',
		'S-202',
		'PRACTICE',
		'Conflicting Security lesson two'
	);

-- Existing schedule of target teacher 1002.
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
) values
	(
		5201,
		2002,
		3003,
		1002,
		'2030-01-10 10:30:00+00',
		'2030-01-10 11:30:00+00',
		'N-301',
		'LECTURE',
		'Target teacher lesson one'
	),
	(
		5202,
		2002,
		3003,
		1002,
		'2030-01-10 14:30:00+00',
		'2030-01-10 15:30:00+00',
		'N-302',
		'LECTURE',
		'Target teacher lesson two'
	);