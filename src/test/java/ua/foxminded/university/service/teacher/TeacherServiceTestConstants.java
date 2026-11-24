package ua.foxminded.university.service.teacher;

import static ua.foxminded.university.model.domain.enums.AcademicRank.LECTURER;
import static ua.foxminded.university.model.domain.enums.AcademicRank.PROFESSOR;

import lombok.experimental.UtilityClass;
import ua.foxminded.university.model.domain.enums.AcademicRank;

@UtilityClass
class TeacherServiceTestConstants {

    final long TEACHER_1_ID = 1101L;
    final long TEACHER_2_ID = 1102L;
    final long TEACHER_3_ID = 1103L;
    final long TEACHER_BLOCKED_ID = 1104L;
    final long TEACHER_DELETED_ID = 1105L;

    final long STUDENT_CS_ID = 1201L;
    final long STUDENT_EE_ID = 1202L;

    final long GROUP_CS_ID = 2001L;
    final long GROUP_EE_ID = 2002L;

    final long MISSING_ID = 999_999L;

    final String TEACHER_1_EMAIL = "teacher.one@example.com";
    final String TEACHER_2_EMAIL = "teacher.two@example.com";
    final String TEACHER_3_EMAIL = "teacher.three@example.com";
    final String TEACHER_BLOCKED_EMAIL = "blocked.teacher@example.com";
    final String TEACHER_DELETED_EMAIL = "deleted.teacher@example.com";

    final String TEACHER_1_FIRST_NAME = "Robert";
    final String TEACHER_1_LAST_NAME = "Miller";

    final AcademicRank TEACHER_1_RANK = LECTURER;
    final AcademicRank TEACHER_2_RANK = PROFESSOR;
    final AcademicRank TEACHER_3_RANK = LECTURER;

    final String TEACHER_1_OFFICE = "A-101";
    final String TEACHER_2_OFFICE = "B-202";
    final String TEACHER_3_OFFICE = "C-303";

    final String TEACHER_CREATE_EMAIL = "new.teacher@example.com";
    final String TEACHER_CREATE_PASSWORD = "Abcd1234!";
    final String TEACHER_CREATE_FIRST_NAME = "New";
    final String TEACHER_CREATE_LAST_NAME = "Teacher";
    final AcademicRank TEACHER_CREATE_RANK = PROFESSOR;
    final String TEACHER_CREATE_OFFICE = "F-606";

    final AcademicRank UPDATED_RANK = PROFESSOR;
    final String UPDATED_OFFICE = "G-707";

    final int ACTIVE_TEACHER_COUNT = 4;
    final int ACTIVE_ENABLED_TEACHER_COUNT = 3;
    final int TEACHER_1_DIRECTORY_COUNT = 2;
    final int ONE_TEACHER = 1;
}