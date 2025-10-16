CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
  teacher_count         int  := 6;
  student_count         int  := 100;
  disabled_teacher_num  int  := 1;
  disabled_student_num  int  := 1;

  fn text[] := ARRAY[
    'Alice','Bob','Charlie','David','Eva','Frank','Grace','Hannah','Ian','Jane',
    'Kevin','Laura','Michael','Nina','Oliver','Pam','Quentin','Rachel','Steve','Tina'
  ];
  ln text[] := ARRAY[
    'Anderson','Brown','Clark','Davis','Evans','Franklin','Garcia','Harris','Ivanov','Johnson',
    'King','Lewis','Martinez','Nelson','Olsen','Perez','Quinn','Roberts','Smith','Turner'
  ];
  fn_len int := array_length(fn, 1);
  ln_len int := array_length(ln, 1);

  group_names text[] := ARRAY['CS-101','CS-102','CS-103','CS-104','CS-105'];

  current_year int := extract(year from now())::int;
  
  t2 BIGINT; t3 BIGINT; t4 BIGINT; t5 BIGINT; t6 BIGINT;
BEGIN
  ----------------------------------------------------------------------
  -- 0) GROUPS
  ----------------------------------------------------------------------
  INSERT INTO groups(name)
  SELECT unnest(group_names)
  ON CONFLICT DO NOTHING;

  ----------------------------------------------------------------------
  -- 1) USERS
  ----------------------------------------------------------------------
  -- TEACHERS: email: teacherN@mail.com, pass: Tch-N!dev
  INSERT INTO app_user (email, password, role, first_name, last_name)
  SELECT
    format('teacher%s@mail.com', s),
    crypt(format('Tch-%s!dev', s), gen_salt('bf', 10)),
    'TEACHER',
    fn[1 + floor(random() * fn_len)::int],
    ln[1 + floor(random() * ln_len)::int]
  FROM generate_series(1, teacher_count) AS s
  ON CONFLICT (email) DO NOTHING;

  -- STUDENTS: email: studentN@mail.com, pass: Std-N!dev
  INSERT INTO app_user (email, password, role, first_name, last_name)
  SELECT
    format('student%s@mail.com', s),
    crypt(format('Std-%s!dev', s), gen_salt('bf', 10)),
    'STUDENT',
    fn[1 + floor(random() * fn_len)::int],
    ln[1 + floor(random() * ln_len)::int]
  FROM generate_series(1, student_count) AS s
  ON CONFLICT (email) DO NOTHING;

  -- DISABLE USERS
  UPDATE app_user
  SET enabled = false
  WHERE email IN (
    format('teacher%s@mail.com', disabled_teacher_num),
    format('student%s@mail.com', disabled_student_num)
  );

  ----------------------------------------------------------------------
  -- 2) TEACHERS
  ----------------------------------------------------------------------
  INSERT INTO teacher (id, academic_rank, office)
  SELECT u.id,
         (ARRAY['LECTURER','SENIOR_LECTURER','PROFESSOR'])[1 + floor(random()*3)::int] AS academic_rank,
         format('B-%s', 100 + row_number() over (order by u.email)) AS office
  FROM app_user u
  WHERE u.role = 'TEACHER'
  ON CONFLICT (id) DO NOTHING;

  ----------------------------------------------------------------------
  -- 3) STUDENTS
  ----------------------------------------------------------------------
  WITH s AS (
    SELECT u.id AS user_id,
           row_number() OVER (ORDER BY random()) - 1 AS rn
    FROM app_user u
    WHERE u.role = 'STUDENT'
  ),
  g AS (
    SELECT id AS group_id,
           row_number() OVER (ORDER BY id) - 1 AS grp,
           count(*) OVER () AS gc
    FROM groups
  )
  INSERT INTO student (id, group_id, enrollment_year)
  SELECT
    s.user_id,
    (SELECT g2.group_id FROM g g2 WHERE g2.grp = s.rn % (SELECT gc FROM g LIMIT 1)),
    current_year
  FROM s
  ON CONFLICT (id) DO NOTHING;
  
  ----------------------------------------------------------------------
  -- 4) COURSES
  ----------------------------------------------------------------------
  SELECT t.id INTO t2 FROM app_user u JOIN teacher t ON t.id = u.id WHERE u.email = 'teacher2@mail.com';
  SELECT t.id INTO t3 FROM app_user u JOIN teacher t ON t.id = u.id WHERE u.email = 'teacher3@mail.com';
  SELECT t.id INTO t4 FROM app_user u JOIN teacher t ON t.id = u.id WHERE u.email = 'teacher4@mail.com';
  SELECT t.id INTO t5 FROM app_user u JOIN teacher t ON t.id = u.id WHERE u.email = 'teacher5@mail.com';
  SELECT t.id INTO t6 FROM app_user u JOIN teacher t ON t.id = u.id WHERE u.email = 'teacher6@mail.com';

  INSERT INTO courses (code, name, description, teacher_id) VALUES
  ('CSE-ALG-101', 'Algorithms and Data Structures (Java)',
   'Overview: Core data structures and algorithmic thinking with hands-on Java practice.
Topics:
- Arrays, lists, stacks, queues
- Hash tables and collision handling
- Trees (BST/AVL), traversals
- Graphs (BFS/DFS)
- Sorting (quick/merge/heap), binary search, two pointers
- Big-O time/space analysis; JUnit tests',
   t2),
  ('CSE-OOP-102', 'Object-Oriented Programming in Java',
   'Overview: Solid OOP foundations and small project design using idiomatic Java.
Topics:
- Classes, interfaces, generics
- Exceptions; equals/hashCode/toString contracts
- Java Collections
- SOLID principles
- Patterns: Strategy, Factory, Builder, Adapter
- JSON with Jackson; simple layered architecture',
   t2),
  ('DB-PSQL-201', 'Databases and SQL (PostgreSQL)',
   'Overview: Relational modeling and practical SQL with performance awareness.
Topics:
- DDL/DML; normalization (1NF–3NF)
- JOINs, aggregations, subqueries, CTE
- Indexes (B-tree, composite, expression)
- Transactions and isolation
- EXPLAIN/ANALYZE basics
- Simple triggers and CHECK constraints',
   t2),
  ('NET-202', 'Computer Networks and Internet Protocols',
   'Overview: How the Internet works from packets to HTTPS.
Topics:
- TCP/IP model; IP/ICMP/TCP/UDP
- DNS, HTTP, HTTPS, TLS
- Routing and NAT
- Proxies and CDNs
- Wireshark, curl, traceroute
- Basic security notions (MITM, certificates)',
   t3),
  ('OS-203', 'Operating Systems (Linux)',
   'Overview: OS internals and APIs for performant applications.
Topics:
- Processes and threads; scheduling
- Virtual memory and paging
- File systems; system calls
- Synchronization primitives
- Monitoring (top, vmstat, iostat)
- Signals and resource limits',
   t3),
  ('SPR-204', 'Web Backend with Spring Boot',
   'Overview: Building REST services with Spring Boot, JPA and basic security.
Topics:
- Controller/service/repository layers
- JPA/Hibernate; transactions
- Bean Validation
- Spring Security (session, CSRF, roles)
- Error handling
- Integration tests; profiles',
   t4),
  ('WEB-205', 'Frontend: HTML/CSS/JS + Bootstrap',
   'Overview: Semantic markup, modern CSS layouts and basic interactivity.
Topics:
- Semantic HTML
- Flexbox and Grid; responsive design
- Bootstrap components
- DOM events and manipulation
- Fetch API and REST integration
- Accessibility basics (a11y)',
   t4),
  ('ML-301', 'Introduction to Machine Learning',
   'Overview: From data preparation to baseline models and validation.
Topics:
- Regression and classification
- Train/validation/test splits
- Feature scaling and pipelines
- Decision trees, Random Forest
- Intro to neural nets
- Metrics: accuracy, ROC-AUC, RMSE',
   t5),
  ('CG-302', 'Computer Graphics and Rendering',
   'Overview: From geometry to pixels, understanding the rendering pipeline.
Topics:
- Transformations and matrices
- Orthographic vs perspective projection
- Rasterization vs ray tracing
- Shader model; textures and normals
- Lighting basics
- WebGL/Three.js demos',
   t6),
  ('SEC-303', 'Application Security Fundamentals',
   'Overview: Practical approach to common application security risks.
Topics:
- Crypto basics (hashing, symmetric/asymmetric)
- Authentication and authorization
- OWASP Top 10 (XSS, SQLi, CSRF, SSRF, IDOR)
- Secure input handling
- Secrets and configuration
- Intro to SAST/DAST tools',
   t6)
  ON CONFLICT (code) DO UPDATE
    SET name        = EXCLUDED.name,
        description = EXCLUDED.description,
        teacher_id  = EXCLUDED.teacher_id;

  ----------------------------------------------------------------------
  -- 5) GROUPS + COURSES
  ----------------------------------------------------------------------
  INSERT INTO group_courses (group_id, course_id)
    SELECT g.id, c.id FROM groups g, courses c
    WHERE g.name = 'CS-101' AND c.code IN ('CSE-OOP-102','DB-PSQL-201','OS-203')
  ON CONFLICT DO NOTHING;

  INSERT INTO group_courses (group_id, course_id)
    SELECT g.id, c.id FROM groups g, courses c
    WHERE g.name = 'CS-102' AND c.code IN ('OS-203','SPR-204','WEB-205')
  ON CONFLICT DO NOTHING;

  INSERT INTO group_courses (group_id, course_id)
    SELECT g.id, c.id FROM groups g, courses c
    WHERE g.name = 'CS-103' AND c.code IN ('CSE-ALG-101','ML-301','CG-302')
  ON CONFLICT DO NOTHING;

  INSERT INTO group_courses (group_id, course_id)
    SELECT g.id, c.id FROM groups g, courses c
    WHERE g.name = 'CS-104' AND c.code IN ('NET-202','OS-203','SEC-303')
  ON CONFLICT DO NOTHING;

  INSERT INTO group_courses (group_id, course_id)
    SELECT g.id, c.id FROM groups g, courses c
    WHERE g.name = 'CS-105' AND c.code IN ('SPR-204','WEB-205','SEC-303')
  ON CONFLICT DO NOTHING;

END $$ LANGUAGE plpgsql;
