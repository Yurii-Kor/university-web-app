package ua.foxminded.university.service.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.teacher.TeacherServiceTestConstants.*;

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
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
		scripts = "/sql/teacher/teacher-service-base.sql",
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
		scripts = "/sql/cleanup-database.sql",
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class TeacherServiceCreateTest {

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("create: happy path — creates teacher")
	void create_happyPath_success() {
		var dto = createDto(
				TEACHER_CREATE_EMAIL);

		var created = teacherService.create(
				dto);

		assertNotNull(
				created.getId());

		var persisted = teacherRepository.findById(
				created.getId())
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						TEACHER_CREATE_EMAIL,
						persisted.getUser().getEmail()),
				() -> assertEquals(
						TEACHER_CREATE_FIRST_NAME,
						persisted.getUser().getFirstName()),
				() -> assertEquals(
						TEACHER_CREATE_LAST_NAME,
						persisted.getUser().getLastName()),
				() -> assertEquals(
						UserRole.TEACHER,
						persisted.getUser().getRole()),
				() -> assertTrue(
						persisted.getUser().isEnabled()),
				() -> assertEquals(
						TEACHER_CREATE_RANK,
						persisted.getAcademicRank()),
				() -> assertEquals(
						TEACHER_CREATE_OFFICE,
						persisted.getOffice()),
				() -> assertTrue(
						passwordEncoder.matches(
								TEACHER_CREATE_PASSWORD,
								persisted.getUser().getPassword()),
						"teacher password must be encoded"));
	}

	@Test
	@DisplayName("create: duplicate email ignoring case -> IllegalArgumentException")
	void create_duplicateEmailIgnoringCase_fails() {
		var dto = createDto(
				TEACHER_1_EMAIL.toUpperCase());

		var exception = assertThrows(
				IllegalArgumentException.class,
				() -> teacherService.create(
						dto));

		assertEquals(
				"An account with this email already exists.",
				exception.getMessage());
	}

	private TeacherCreateDto createDto(
			String email) {

		return new TeacherCreateDto(
				email,
				TEACHER_CREATE_PASSWORD,
				TEACHER_CREATE_FIRST_NAME,
				TEACHER_CREATE_LAST_NAME,
				TEACHER_CREATE_RANK,
				TEACHER_CREATE_OFFICE);
	}
}