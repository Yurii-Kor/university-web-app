create table if not exists app_user (
  id          bigserial    primary key,
  email       varchar(255) not null,
  password    varchar(100) not null, 
  role        varchar(16)  not null, 
  first_name  varchar(64)  not null,
  last_name   varchar(64)  not null,
  enabled     boolean      not null default true,
  created_at  timestamptz  not null default now(),
  deleted_at  timestamptz,

  constraint ck_app_user_role        check (role in ('STUDENT','TEACHER','ADMIN')),
  constraint ck_app_user_email_lower check (email = lower(email)),
  constraint uq_app_user_email       unique (email)
);

create table if not exists groups (
  id          bigserial    primary key,
  name        varchar(255) not null,
  deleted_at       timestamptz
);
create unique index if not exists uq_groups_name_ci on groups (lower(name));

create table if not exists student (
  id               bigint primary key references app_user(id) on delete cascade,
  group_id         bigint references groups(id) not null,
  enrollment_year  int not null,
  deleted_at       timestamptz
);
create index if not exists idx_student_group on student(group_id);

create table if not exists teacher (
  id            bigint primary key references app_user(id) on delete cascade,
  academic_rank varchar(64) not null,
  office        varchar(64) not null,
  deleted_at       timestamptz
);

create table if not exists courses (
  id          bigserial primary key,
  code        varchar(32)  not null unique,
  name        varchar(255) not null unique,
  description text,
  teacher_id  bigint  references teacher(id)
);
create index if not exists idx_courses__teacher_id on courses(teacher_id);
create unique index if not exists uq_courses_code_ci on courses (lower(code));
create unique index if not exists uq_courses_name_ci on courses (lower(name));

create table if not exists group_courses (
  group_id  bigint not null references groups(id),
  course_id bigint not null references courses(id),
  primary key (group_id, course_id)
);
create index if not exists idx_group_courses_course on group_courses(course_id);

create table if not exists lessons (
  id                 bigserial    primary key,
  group_id           bigint       not null,
  course_id          bigint       not null,
  start_time         timestamptz  not null,
  end_time           timestamptz  not null,
  room               varchar(64)  not null,
  lesson_type        varchar(32)  not null,
  description text,

  constraint fk_lesson_group   foreign key (group_id)  references groups(id),
  constraint fk_lesson_course  foreign key (course_id) references courses(id),

  constraint fk_lesson_pair foreign key (group_id, course_id)
    references group_courses(group_id, course_id),

  constraint ck_time_order        check (end_time > start_time),
  constraint ck_lesson_type       check (lesson_type in ('LECTURE','PRACTICE','LAB','SEMINAR','EXAM', 'OTHER'))
);

create index if not exists idx_lesson_group_start
  on lessons (group_id, start_time);

create index if not exists idx_lesson_group_end
  on lessons (group_id, end_time);

create index if not exists idx_lesson_course_start
  on lessons (course_id, start_time);

create index if not exists idx_lesson_room_ci_start
  on lessons ((upper(room)), start_time);

create index if not exists idx_lesson_start_time
  on lessons (start_time);
