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
    'group.teacher.one@example.com',
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
    'group.teacher.two@example.com',
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
    'group.student.one@example.com',
    'Abcd1234!',
    'STUDENT',
    'Grace',
    'Hopper',
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
    1004,
    'group.student.two@example.com',
    'Abcd1234!',
    'STUDENT',
    'Barbara',
    'Liskov',
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
    1005,
    'group.student.deleted@example.com',
    'Abcd1234!',
    'STUDENT',
    'Deleted',
    'Student',
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
    'CS-101'
);

insert into groups (
    id,
    name
)
values (
    2002,
    'SE-205'
);

insert into groups (
    id,
    name
)
values (
    2003,
    'EE-205'
);

insert into groups (
    id,
    name
)
values (
    2004,
    'ME-101'
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

insert into student (
    id,
    group_id,
    enrollment_year
)
values (
    1004,
    2001,
    2024
);

insert into student (
    id,
    group_id,
    enrollment_year,
    deleted_at
)
values (
    1005,
    2002,
    2024,
    now()
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
    'CSE-OS-302',
    'Operating Systems',
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
    3002,
    'SEC-303',
    'Security',
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
    'DB-201',
    'Databases',
    null,
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
    3002
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
    3001
);