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
    'primary.admin@example.com',
    'Abcd1234!',
    'ADMIN',
    'Alice',
    'Admin',
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
    'second.admin@example.com',
    'Abcd1234!',
    'ADMIN',
    'Bob',
    'Builder',
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
    'disabled.self@example.com',
    'Abcd1234!',
    'ADMIN',
    'Carol',
    'Clark',
    false
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
    'third.admin@example.com',
    'Abcd1234!',
    'ADMIN',
    'David',
    'Doe',
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
    'disabled.admin@example.com',
    'Abcd1234!',
    'ADMIN',
    'Eve',
    'Evans',
    false
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
    2001,
    'student@example.com',
    'Abcd1234!',
    'STUDENT',
    'Grace',
    'Hopper',
    true
);