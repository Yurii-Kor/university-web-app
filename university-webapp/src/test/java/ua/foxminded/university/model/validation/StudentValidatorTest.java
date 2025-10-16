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
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class StudentValidatorTest {

	private static final String CASE_PATTERN = "user: {0}, group: {1}, year: {2} -> valid={3}";

	public static final Integer VALID_ENROLLMENT_YEAR = 2024;
	public static final Integer TOO_EARLY_ENROLLMENT = 1999;
	public static final Integer TOO_LATE_ENROLLMENT = 2101;

	private static final StudyGroup DUMMY_GROUP = StudyGroup.builder().id(1L).name("CS-101").build();

	private static final AppUser VALID_STUDENT_USER = AppUser.builder()
			.id(101L)
			.email("student.valid@example.com")
			.password("Strong#Pass1")
			.role(UserRole.STUDENT)
			.firstName("John")
			.lastName("Doe")
			.enabled(true)
			.build();

	private static final AppUser VALID_TEACHER_USER = AppUser.builder()
			.id(201L)
			.email("teacher.valid@example.com")
			.password("Strong#Pass2")
			.role(UserRole.TEACHER)
			.firstName("Alice")
			.lastName("Smith")
			.enabled(true)
			.build();

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("Student entity bean validation")
	void validateStudent(AppUser user, StudyGroup group, Integer year, boolean shouldPass) {
		Student student = Student.builder()
				.id(user != null ? user.getId() : null)
				.user(user)
				.group(group)
				.enrollmentYear(year)
				.build();

		Set<ConstraintViolation<Student>> violations = validator.validate(student);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(VALID_STUDENT_USER, DUMMY_GROUP, VALID_ENROLLMENT_YEAR, true),

				Arguments.of(null, DUMMY_GROUP, VALID_ENROLLMENT_YEAR, false),
				Arguments.of(VALID_STUDENT_USER, null, VALID_ENROLLMENT_YEAR, false),
				Arguments.of(VALID_STUDENT_USER, DUMMY_GROUP, null, false),

				Arguments.of(VALID_STUDENT_USER, DUMMY_GROUP, TOO_EARLY_ENROLLMENT, false),
				Arguments.of(VALID_STUDENT_USER, DUMMY_GROUP, TOO_LATE_ENROLLMENT, false),

				Arguments.of(VALID_TEACHER_USER, DUMMY_GROUP, VALID_ENROLLMENT_YEAR, false));
	}
}
