----------------------------------------------------------------------
-- 1) ADD ASSIGNED TEACHER TO LESSONS
----------------------------------------------------------------------

ALTER TABLE lessons
  ADD COLUMN teacher_id bigint;

----------------------------------------------------------------------
-- Existing lessons were previously associated with a teacher only
-- through their course.
--
-- Preserve existing data by assigning the current course teacher
-- to every existing lesson before making teacher_id mandatory.
----------------------------------------------------------------------

UPDATE lessons l
SET teacher_id = c.teacher_id
FROM courses c
WHERE c.id = l.course_id;

----------------------------------------------------------------------
-- teacher_id is required by the new lesson model.
--
-- Fail the migration explicitly if some existing lesson belongs to
-- a course without an assigned teacher.
----------------------------------------------------------------------

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM lessons
    WHERE teacher_id IS NULL
  ) THEN
    RAISE EXCEPTION
      'Cannot migrate lessons: some lessons belong to courses without an assigned teacher';
  END IF;
END $$;

ALTER TABLE lessons
  ALTER COLUMN teacher_id SET NOT NULL;

ALTER TABLE lessons
  ADD CONSTRAINT fk_lesson_teacher
  FOREIGN KEY (teacher_id)
  REFERENCES teacher(id);

CREATE INDEX idx_lesson_teacher_start
  ON lessons (teacher_id, start_time);

----------------------------------------------------------------------
-- 2) UPDATE GROUP-COURSE DELETE BEHAVIOUR
----------------------------------------------------------------------

ALTER TABLE group_courses
  DROP CONSTRAINT group_courses_group_id_fkey;

ALTER TABLE group_courses
  ADD CONSTRAINT group_courses_group_id_fkey
  FOREIGN KEY (group_id)
  REFERENCES groups(id)
  ON DELETE CASCADE;

ALTER TABLE group_courses
  DROP CONSTRAINT group_courses_course_id_fkey;

ALTER TABLE group_courses
  ADD CONSTRAINT group_courses_course_id_fkey
  FOREIGN KEY (course_id)
  REFERENCES courses(id)
  ON DELETE CASCADE;

----------------------------------------------------------------------
-- 3) UPDATE LESSON RELATION TO GROUP_COURSES
--
-- The composite relation already guarantees that both group_id and
-- course_id belong to valid entities through group_courses, therefore
-- the old individual foreign keys are redundant.
----------------------------------------------------------------------

ALTER TABLE lessons
  DROP CONSTRAINT fk_lesson_group;

ALTER TABLE lessons
  DROP CONSTRAINT fk_lesson_course;

ALTER TABLE lessons
  DROP CONSTRAINT fk_lesson_pair;

ALTER TABLE lessons
  ADD CONSTRAINT fk_lesson_pair
  FOREIGN KEY (group_id, course_id)
  REFERENCES group_courses(group_id, course_id)
  ON DELETE CASCADE;