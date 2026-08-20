package ua.foxminded.university.service.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.student.StudentServiceTestConstants.*;

import java.time.Year;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.student.StudentRepository;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.student.exception.StudentCreateException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
		scripts = "/sql/student/student-service-base.sql",
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
		scripts = "/sql/cleanup-database.sql",
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class StudentServiceCreateTest {

	@Autowired
	private StudentService studentService;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("create: happy path — creates student")
	void create_happyPath_success() {
		var dto = createDto(
				GROUP_ME_ID,
				STUDENT_CREATE_ENROLLMENT_YEAR);

		var created = studentService.create(dto);

		assertNotNull(created.getId());

		var persisted = studentRepository.findById(
				created.getId())
				.orElseThrow();

		assertEquals(
				STUDENT_CREATE_EMAIL,
				persisted.getUser().getEmail());

		assertEquals(
				STUDENT_CREATE_FIRST_NAME,
				persisted.getUser().getFirstName());

		assertEquals(
				STUDENT_CREATE_LAST_NAME,
				persisted.getUser().getLastName());

		assertEquals(
				UserRole.STUDENT,
				persisted.getUser().getRole());

		assertTrue(
				persisted.getUser().isEnabled());

		assertEquals(
				GROUP_ME_ID,
				persisted.getGroup().getId());

		assertEquals(
				STUDENT_CREATE_ENROLLMENT_YEAR,
				persisted.getEnrollmentYear());

		assertTrue(
				passwordEncoder.matches(
						STUDENT_CREATE_PASSWORD,
						persisted.getUser().getPassword()),
				"student password must be encoded");
	}

	@Test
	@DisplayName("create: null enrollment year -> uses current year")
	void create_nullEnrollmentYear_usesCurrentYear() {
		var expectedYear = Year.now()
				.getValue();

		var dto = createDto(
				GROUP_ME_ID,
				null);

		var created = studentService.create(dto);

		assertEquals(
				expectedYear,
				created.getEnrollmentYear());
	}

	@Test
	@DisplayName("create: duplicate email -> StudentCreateException")
	void create_duplicateEmail_fails() {
		var dto = new StudentCreateDto(
				STUDENT_1_EMAIL,
				STUDENT_CREATE_PASSWORD,
				STUDENT_CREATE_FIRST_NAME,
				STUDENT_CREATE_LAST_NAME,
				GROUP_ME_ID,
				STUDENT_CREATE_ENROLLMENT_YEAR);

		var exception = assertThrows(
				StudentCreateException.class,
				() -> studentService.create(dto));

		assertEquals(
				"email",
				exception.getField());

		assertEquals(
				dto,
				exception.getForm());

		assertEquals(
				"An account with this email already exists.",
				exception.getMessage());
	}

	@Test
	@DisplayName("create: missing group -> StudentCreateException")
	void create_missingGroup_fails() {
		var dto = createDto(
				MISSING_ID,
				STUDENT_CREATE_ENROLLMENT_YEAR);

		var exception = assertThrows(
				StudentCreateException.class,
				() -> studentService.create(dto));

		assertEquals(
				"groupId",
				exception.getField());

		assertEquals(
				dto,
				exception.getForm());

		assertEquals(
				"Selected study group is no longer available.",
				exception.getMessage());
	}

	private StudentCreateDto createDto(
			long groupId,
			Integer enrollmentYear) {

		return new StudentCreateDto(
				STUDENT_CREATE_EMAIL,
				STUDENT_CREATE_PASSWORD,
				STUDENT_CREATE_FIRST_NAME,
				STUDENT_CREATE_LAST_NAME,
				groupId,
				enrollmentYear);
	}
}