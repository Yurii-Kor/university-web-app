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
    'admin.test@example.com',
    '$2a$10$zuVKu2I7XCxtiSIIJIcWUurJKhS9FMsanCH50hsN4paQ2VzFqnpNm',
    'ADMIN',
    'Alice',
    'Cooper',
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
    'student.taken@example.com',
    '$2a$10$zuVKu2I7XCxtiSIIJIcWUurJKhS9FMsanCH50hsN4paQ2VzFqnpNm',
    'STUDENT',
    'Bob',
    'User',
    true
);