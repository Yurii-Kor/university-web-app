package ua.foxminded.university.web.lesson;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.experimental.UtilityClass;

@UtilityClass
class ScheduleControllerWebMvcTestConstants {

    final long ADMIN_ID = 41L;
    final long TEACHER_ID = 42L;
    final long STUDENT_ID = 43L;

    final long GROUP_ID = 2001L;
    final long GROUP_STUDENT_COUNT = 20L;

    final long TEACHER_LESSON_ID = 4001L;
    final long STUDENT_LESSON_ID = 4002L;

    final long COURSE_ID = 3001L;

    final String COURSE_CODE = "CS-101";
    final String COURSE_NAME = "Java Programming";

    final String GROUP_NAME = "SE-21";

    final String TEACHER_FIRST_NAME = "Alice";
    final String TEACHER_LAST_NAME = "Teacher";

    final String ROOM = "A-101";

    final String DAILY_LESSON_DESCRIPTION = "Daily lesson";

    final LocalDate REQUESTED_DATE =
            LocalDate.of(
                    2030,
                    1,
                    10);

    final OffsetDateTime DAY_FROM =
            OffsetDateTime.of(
                    2030,
                    1,
                    10,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    final OffsetDateTime DAY_TO =
            DAY_FROM.plusDays(1);

    final OffsetDateTime WEEK_FROM =
            OffsetDateTime.of(
                    2030,
                    1,
                    7,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    final OffsetDateTime WEEK_TO =
            OffsetDateTime.of(
                    2030,
                    1,
                    14,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    final OffsetDateTime TEACHER_LESSON_START =
            OffsetDateTime.of(
                    2030,
                    1,
                    10,
                    10,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    final OffsetDateTime TEACHER_LESSON_END =
            TEACHER_LESSON_START.plusHours(1);

    final OffsetDateTime STUDENT_LESSON_START =
            OffsetDateTime.of(
                    2030,
                    1,
                    11,
                    12,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    final OffsetDateTime STUDENT_LESSON_END =
            STUDENT_LESSON_START.plusHours(1);

    final long CREATE_ADMIN_ID = 44L;
    final long CREATE_LESSON_ID = 4001L;

    final LocalDate CREATE_DATE =
            LocalDate.of(
                    2030,
                    1,
                    11);

    final LocalTime CREATE_START_TIME =
            LocalTime.of(
                    10,
                    0);

    final LocalTime CREATE_END_TIME =
            LocalTime.of(
                    11,
                    0);

    final String CREATE_DESCRIPTION =
            "MVC test lesson";

    final String CREATE_POLICY_MESSAGE =
            "Lesson start time must be in the future.";
}