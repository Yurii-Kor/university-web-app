package ua.foxminded.university.model.persistence.lesson;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;

@UtilityClass
class LessonRepositoryTestConstants {

    final long TEACHER_1_ID = 1101L;
    final long TEACHER_2_ID = 1102L;

    final long GROUP_1_ID = 2001L;
    final long GROUP_2_ID = 2002L;
    final long GROUP_3_ID = 2003L;

    final long COURSE_1_ID = 3001L;
    final long COURSE_2_ID = 3002L;
    final long COURSE_3_ID = 3003L;

    final long LESSON_BASE_ID = 4001L;
    final long LESSON_GROUP_OVERLAP_ID = 4002L;
    final long LESSON_TEACHER_OVERLAP_ID = 4003L;
    final long LESSON_ROOM_OVERLAP_ID = 4004L;
    final long LESSON_ADJACENT_BEFORE_ID = 4005L;
    final long LESSON_ADJACENT_AFTER_ID = 4006L;
    final long LESSON_FUTURE_ID = 4007L;
    final long LESSON_AT_UPPER_BOUND_ID = 4008L;

    final long MISSING_ID = 999_999L;

    final String GROUP_1_NAME = "CS-101";
    final String GROUP_2_NAME = "SE-202";
    final String GROUP_3_NAME = "EE-303";

    final String COURSE_1_CODE = "CSE-JAVA";
    final String COURSE_1_NAME = "Java Programming";

    final String COURSE_2_CODE = "CSE-DB";
    final String COURSE_2_NAME = "Database Systems";

    final String COURSE_3_CODE = "NET-101";
    final String COURSE_3_NAME = "Computer Networks";

    final String TEACHER_1_FIRST_NAME = "Alice";
    final String TEACHER_1_LAST_NAME = "Anderson";

    final String TEACHER_2_FIRST_NAME = "Bob";
    final String TEACHER_2_LAST_NAME = "Brown";

    final String ROOM_BASE = "A-101";
    final String ROOM_GROUP_OVERLAP = "B-201";
    final String ROOM_TEACHER_OVERLAP = "C-301";
    final String ROOM_ROOM_OVERLAP = "a-101";

    final String BASE_DESCRIPTION =
            "Core Java lecture";

    final String GROUP_OVERLAP_DESCRIPTION =
            "Group overlap lesson";

    final String TEACHER_OVERLAP_DESCRIPTION =
            "Teacher overlap lesson";

    final String ROOM_OVERLAP_DESCRIPTION =
            "Room overlap lesson";

    final OffsetDateTime SCHEDULE_FROM =
            OffsetDateTime.parse(
                    "2030-01-10T10:00:00Z");

    final OffsetDateTime SCHEDULE_TO =
            OffsetDateTime.parse(
                    "2030-01-10T12:00:00Z");

    final OffsetDateTime BASE_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:00:00Z");

    final OffsetDateTime BASE_END =
            OffsetDateTime.parse(
                    "2030-01-10T11:00:00Z");

    final OffsetDateTime GROUP_OVERLAP_LESSON_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:30:00Z");

    final OffsetDateTime GROUP_OVERLAP_LESSON_END =
            OffsetDateTime.parse(
                    "2030-01-10T11:30:00Z");

    final OffsetDateTime TEACHER_OVERLAP_LESSON_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:15:00Z");

    final OffsetDateTime TEACHER_OVERLAP_LESSON_END =
            OffsetDateTime.parse(
                    "2030-01-10T10:45:00Z");

    final OffsetDateTime ROOM_OVERLAP_LESSON_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:20:00Z");

    final OffsetDateTime ROOM_OVERLAP_LESSON_END =
            OffsetDateTime.parse(
                    "2030-01-10T10:50:00Z");

    final OffsetDateTime FUTURE_START =
            OffsetDateTime.parse(
                    "2030-01-11T09:00:00Z");

    final OffsetDateTime FUTURE_END =
            OffsetDateTime.parse(
                    "2030-01-11T10:00:00Z");

    final OffsetDateTime FUTURE_CUTOFF =
            OffsetDateTime.parse(
                    "2030-01-10T12:00:00Z");

    final OffsetDateTime GROUP_OVERLAP_QUERY_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:45:00Z");

    final OffsetDateTime GROUP_OVERLAP_QUERY_END =
            OffsetDateTime.parse(
                    "2030-01-10T11:15:00Z");

    final OffsetDateTime CONFLICT_QUERY_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:30:00Z");

    final OffsetDateTime CONFLICT_QUERY_END =
            OffsetDateTime.parse(
                    "2030-01-10T10:40:00Z");

    final OffsetDateTime ADJACENT_WINDOW_START =
            OffsetDateTime.parse(
                    "2030-01-10T10:00:00Z");

    final OffsetDateTime ADJACENT_WINDOW_END =
            OffsetDateTime.parse(
                    "2030-01-10T11:00:00Z");
}