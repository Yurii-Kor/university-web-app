package ua.foxminded.university.web.teacher;

import lombok.experimental.UtilityClass;
import ua.foxminded.university.model.domain.enums.AcademicRank;

@UtilityClass
class TeachersManagementWebMvcTestConstants {

    final long ADMIN_ID = 41L;
    final long TEACHER_USER_ID = 42L;
    final long STUDENT_USER_ID = 43L;

    final long TEACHER_ID = 1001L;

    final String TEACHER_EMAIL = "alice.teacher@example.com";
    final String TEACHER_PASSWORD = "Password1!";
    final String TEACHER_FIRST_NAME = "Alice";
    final String TEACHER_LAST_NAME = "Teacher";

    final AcademicRank TEACHER_RANK =
            AcademicRank.LECTURER;

    final AcademicRank UPDATED_RANK =
            AcademicRank.PROFESSOR;

    final String TEACHER_OFFICE = "B-105";
    final String UPDATED_OFFICE = "C-202";

    final String INVALID_OFFICE = "bad-office";
}