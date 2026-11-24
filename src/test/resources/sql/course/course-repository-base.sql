insert into app_user (
    id,
    email,
    password,
    role,
    first_name,
    last_name,
    enabled
)
values (
    1001,
    'ada.teacher@example.com',
    'Abcd1234!',
    'TEACHER',
    'Ada',
    'Lovelace',
    true
);

insert into app_user (
    id,
    email,
    password,
    role,
    first_name,
    last_name,
    enabled
)
values (
    1002,
    'alan.teacher@example.com',
    'Abcd1234!',
    'TEACHER',
    'Alan',
    'Turing',
    true
);

insert into app_user (
    id,
    email,
    password,
    role,
    first_name,
    last_name,
    enabled
)
values (
    1003,
    'grace.student@example.com',
    'Abcd1234!',
    'STUDENT',
    'Grace',
    'Hopper',
    true
);

insert into teacher (
    id,
    academic_rank,
    office
)
values (
    1001,
    'PROFESSOR',
    'A-101'
);

insert into teacher (
    id,
    academic_rank,
    office
)
values (
    1002,
    'LECTURER',
    'B-202'
);

insert into groups (
    id,
    name
)
values (
    2001,
    'CS-201'
);

insert into groups (
    id,
    name
)
values (
    2002,
    'CS-202'
);

insert into student (
    id,
    group_id,
    enrollment_year
)
values (
    1003,
    2001,
    2024
);

insert into courses (
    id,
    code,
    name,
    description,
    teacher_id
)
values (
    3001,
    'CSE-B-200',
    'Operating Systems',
    'Operating systems course',
    1001
);

insert into courses (
    id,
    code,
    name,
    description,
    teacher_id
)
values (
    3002,
    'CSE-C-300',
    'Databases',
    null,
    1001
);

insert into courses (
    id,
    code,
    name,
    description,
    teacher_id
)
values (
    3003,
    'CSE-A-100',
    'Algorithms',
    'Algorithms course',
    1002
);

insert into group_courses (
    group_id,
    course_id
)
values (
    2001,
    3001
);

insert into group_courses (
    group_id,
    course_id
)
values (
    2001,
    3003
);

insert into group_courses (
    group_id,
    course_id
)
values (
    2002,
    3002
);