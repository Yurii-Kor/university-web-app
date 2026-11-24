package ua.foxminded.university.web.student;

import lombok.experimental.UtilityClass;

@UtilityClass
class StudentsManagementWebMvcTestConstants {

    final long ADMIN_ID = 41L;
    final long TEACHER_ID = 42L;
    final long STUDENT_USER_ID = 43L;

    final long STUDENT_ID = 1001L;

    final long GROUP_CS_ID = 2001L;
    final long GROUP_SE_ID = 2002L;
    final long MISSING_GROUP_ID = 9999L;

    final String GROUP_CS = "CS-101";
    final String GROUP_SE = "SE-205";

    final String STUDENT_EMAIL = "alice.student@example.com";
    final String STUDENT_PASSWORD = "Password1!";
    final String STUDENT_FIRST_NAME = "Alice";
    final String STUDENT_LAST_NAME = "Student";

    final int STUDENT_ENROLLMENT_YEAR = 2026;
    final int UPDATED_ENROLLMENT_YEAR = 2025;
}