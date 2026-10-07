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
    'assessment.student@example.com',
    'Abcd1234!',
    'STUDENT',
    'Role',
    'Assessor',
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
    'assessment.teacher@example.com',
    'Abcd1234!',
    'TEACHER',
    'Role',
    'Assessor',
    true
);

insert into groups (
    id,
    name
)
values (
    2001,
    'RPC-101'
);

insert into student (
    id,
    group_id,
    enrollment_year
)
values (
    1001,
    2001,
    2024
);

insert into teacher (
    id,
    academic_rank,
    office
)
values (
    1002,
    'LECTURER',
    'A-101'
);