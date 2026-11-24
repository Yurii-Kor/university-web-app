package ua.foxminded.university.service.student;

import lombok.experimental.UtilityClass;

@UtilityClass
class StudentServiceTestConstants {

    final long STUDENT_1_ID = 1001L;
    final long STUDENT_2_ID = 1002L;
    final long STUDENT_BLOCKED_ID = 1003L;
    final long STUDENT_DELETED_ID = 1004L;
    final long STUDENT_3_ID = 1005L;
    final long STUDENT_4_ID = 1006L;

    final long TEACHER_1_ID = 1101L;
    final long TEACHER_2_ID = 1102L;

    final long GROUP_CS_ID = 2001L;
    final long GROUP_SE_ID = 2002L;
    final long GROUP_EE_ID = 2003L;
    final long GROUP_ME_ID = 2004L;

    final long MISSING_ID = 999_999L;

    final String GROUP_CS = "CS-101";
    final String GROUP_SE = "SE-205";
    final String GROUP_EE = "EE-205";
    final String GROUP_ME = "ME-101";

    final String STUDENT_1_EMAIL = "john.smith@example.com";
    final String STUDENT_2_EMAIL = "alice.brown@example.com";
    final String STUDENT_BLOCKED_EMAIL = "blocked.student@example.com";
    final String STUDENT_DELETED_EMAIL = "deleted.student@example.com";
    final String STUDENT_3_EMAIL = "david.wilson@example.com";
    final String STUDENT_4_EMAIL = "emma.davis@example.com";

    final String STUDENT_1_FIRST_NAME = "John";
    final String STUDENT_1_LAST_NAME = "Smith";

    final int STUDENT_1_ENROLLMENT_YEAR = 2024;

    final String STUDENT_CREATE_EMAIL = "new.student@example.com";
    final String STUDENT_CREATE_PASSWORD = "Abcd1234!";
    final String STUDENT_CREATE_FIRST_NAME = "New";
    final String STUDENT_CREATE_LAST_NAME = "Student";
    final int STUDENT_CREATE_ENROLLMENT_YEAR = 2026;
    final int UPDATED_ENROLLMENT_YEAR = 2027;

    final int ACTIVE_STUDENT_COUNT = 5;
    final int ACTIVE_ENABLED_STUDENT_COUNT = 4;
    final int TEACHER_1_STUDENT_COUNT = 3;
    final int ONE_STUDENT = 1;
}