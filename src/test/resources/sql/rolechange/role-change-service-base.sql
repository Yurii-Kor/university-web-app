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
    'role.student@example.com',
    'Abcd1234!',
    'STUDENT',
    'Role',
    'Changer',
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
    'role.teacher@example.com',
    'Abcd1234!',
    'TEACHER',
    'Role',
    'Changer',
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
    'role.admin@example.com',
    'Abcd1234!',
    'ADMIN',
    'Role',
    'Admin',
    true
);

insert into groups (
    id,
    name
)
values (
    2001,
    'RC-101'
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