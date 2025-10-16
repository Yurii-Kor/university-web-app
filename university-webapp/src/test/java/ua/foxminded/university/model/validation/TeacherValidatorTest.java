package ua.foxminded.university.model.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class TeacherValidatorTest {

	private static final String CASE_PATTERN = "user: {0}, rank: {1}, office: {2} -> valid={3}";

	public static final String VALID_OFFICE = "B-105";
	public static final String INVALID_OFFICE_LOWERCASE_LETTER = "b-105";
	public static final String INVALID_OFFICE_NO_DASH = "B105";
	public static final String INVALID_OFFICE_NOT_THREE_DIGITS = "B-12";
	public static final String EMPTY_OFFICE = "";

	private static final AppUser VALID_TEACHER_USER = AppUser.builder()
			.id(201L)
			.email("teacher.valid@example.com")
			.password("Strong#Pass2")
			.role(UserRole.TEACHER)
			.firstName("Alice")
			.lastName("Smith")
			.enabled(true)
			.build();

	private static final AppUser VALID_STUDENT_USER = AppUser.builder()
			.id(101L)
			.email("student.valid@example.com")
			.password("Strong#Pass1")
			.role(UserRole.STUDENT)
			.firstName("John")
			.lastName("Doe")
			.enabled(true)
			.build();

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("Teacher entity bean validation")
	void validateStudent(AppUser user, AcademicRank rank, String office, boolean shouldPass) {
		Teacher teacher = Teacher.builder()
				.id(user != null ? user.getId() : null)
				.user(user)
				.academicRank(rank)
				.office(office)
				.build();

		Set<ConstraintViolation<Teacher>> violations = validator.validate(teacher);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(VALID_TEACHER_USER, AcademicRank.LECTURER, VALID_OFFICE, true),

				Arguments.of(null, AcademicRank.LECTURER, VALID_OFFICE, false),
				Arguments.of(VALID_STUDENT_USER, AcademicRank.LECTURER, VALID_OFFICE, false),
				Arguments.of(VALID_TEACHER_USER, null, VALID_OFFICE, false),
				Arguments.of(VALID_TEACHER_USER, AcademicRank.PROFESSOR, null, false),
				Arguments.of(VALID_TEACHER_USER, AcademicRank.PROFESSOR, EMPTY_OFFICE, false),
				Arguments.of(VALID_TEACHER_USER, AcademicRank.SENIOR_LECTURER, INVALID_OFFICE_LOWERCASE_LETTER, false),
				Arguments.of(VALID_TEACHER_USER, AcademicRank.SENIOR_LECTURER, INVALID_OFFICE_NO_DASH, false),
				Arguments.of(VALID_TEACHER_USER, AcademicRank.SENIOR_LECTURER, INVALID_OFFICE_NOT_THREE_DIGITS, false));
	}
}
