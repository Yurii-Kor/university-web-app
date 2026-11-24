package ua.foxminded.university.service.course;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class CourseServiceTestConstants {

    static final long TEST_TEACHER_ID = 1001L;
    static final long OTHER_TEACHER_ID = 1002L;
    static final long STUDENT_IN_G1_ID = 1003L;

    static final long GROUP_1_ID = 2001L;
    static final long GROUP_2_ID = 2002L;

    static final long COURSE_OS_ID = 3001L;
    static final long COURSE_SECURITY_ID = 3002L;
    static final long COURSE_TARGET_TEACHER_ID = 3003L;

    static final long OS_PAST_LESSON_ID = 5001L;
    static final long OS_ONGOING_LESSON_ID = 5002L;
    static final long OS_FUTURE_LESSON_1_ID = 5003L;
    static final long OS_FUTURE_LESSON_2_ID = 5004L;

    static final long SECURITY_FUTURE_LESSON_1_ID = 5101L;
    static final long SECURITY_FUTURE_LESSON_2_ID = 5102L;

    static final long TARGET_TEACHER_LESSON_1_ID = 5201L;
    static final long TARGET_TEACHER_LESSON_2_ID = 5202L;

    static final String CODE_ALG_UPPER = "CSE-ALG-101";
    static final String CODE_SEC_UPPER = "SEC-303";
    static final String CODE_DB_UPPER = "DB-PSQL-201";
    static final String CODE_OS_UPPER = "CSE-OS-302";
    static final String BAD_CODE = "bad";
    static final String TO_DELETE_CODE = "CSE-100";
    static final String TO_CREATE_CODE = "SEC-420";

    static final String COURSE_ALG = "Algorithms 101";
    static final String COURSE_SECURITY = "Security";
    static final String COURSE_SECURITY_LOWER = "security";
    static final String COURSE_DATABASES = "Databases";
    static final String COURSE_OS = "Operating Systems";

    static final String TO_DELETE_NAME = "To Delete";
    static final String TO_CREATE_NAME = "To Create";

    static final String TEST_TEACHER_FIRST_NAME = "Ada";
    static final String TEST_TEACHER_LAST_NAME = "Lovelace";
    static final String TEST_TEACHER_EMAIL = "teacher@example.com";

    static final String OTHER_TEACHER_FIRST_NAME = "Alan";
    static final String OTHER_TEACHER_LAST_NAME = "Turing";
    static final String OTHER_TEACHER_EMAIL = "teacher2@example.com";

    static final String DESC_A = "  Intro to algorithms ";
    static final String DESC_BLANK = "   ";

    static final long NON_EXISTENT_ID = 999_999L;

    static final int ONE_COURSE = 1;

    static final OffsetDateTime TEST_NOW =
            OffsetDateTime.of(
                    2030,
                    1,
                    10,
                    9,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    static final OffsetDateTime REASSIGNMENT_RANGE_START =
            OffsetDateTime.of(
                    2030,
                    1,
                    9,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    static final OffsetDateTime REASSIGNMENT_RANGE_END =
            OffsetDateTime.of(
                    2030,
                    1,
                    12,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    private CourseServiceTestConstants() {
    }
}