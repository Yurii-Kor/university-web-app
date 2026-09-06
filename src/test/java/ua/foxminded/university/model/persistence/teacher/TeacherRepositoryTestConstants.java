package ua.foxminded.university.model.persistence.teacher;

import lombok.experimental.UtilityClass;
import ua.foxminded.university.model.domain.enums.AcademicRank;

@UtilityClass
class TeacherRepositoryTestConstants {

	final long TEACHER_1_ID = 1101L;
	final long TEACHER_2_ID = 1102L;
	final long TEACHER_3_ID = 1103L;
	final long TEACHER_BLOCKED_ID = 1104L;
	final long TEACHER_DELETED_ID = 1105L;

	final long STUDENT_CS_ID = 1201L;
	final long STUDENT_EE_ID = 1202L;

	final long MISSING_ID = 999_999L;

	final String TEACHER_1_EMAIL = "teacher.one@example.com";
	final String TEACHER_1_FIRST_NAME = "Robert";
	final String TEACHER_1_LAST_NAME = "Miller";
	final AcademicRank TEACHER_1_RANK = AcademicRank.LECTURER;
	final String TEACHER_1_OFFICE = "A-101";

	final String TEACHER_2_EMAIL = "teacher.two@example.com";
	final AcademicRank TEACHER_2_RANK = AcademicRank.PROFESSOR;
	final String TEACHER_2_OFFICE = "B-202";

	final int ACTIVE_TEACHER_COUNT = 4;
	final int ACTIVE_ENABLED_TEACHER_COUNT = 3;
	final int TEACHER_1_COURSES_COUNT = 2;
}