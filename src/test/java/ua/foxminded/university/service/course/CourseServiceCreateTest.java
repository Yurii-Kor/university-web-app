package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.course.exception.CourseCreateException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	scripts = "/sql/course/course-service-base.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CourseServiceCreateTest {

	@Autowired
	private CourseService courseService;

	@Test
	@DisplayName("create: happy path — creates course with teacher and normalized name")
	void create_happyPath_success() {
		var dto = new CourseCreateDto(
				CODE_ALG_UPPER,
				COURSE_ALG,
				DESC_A,
				TEST_TEACHER_ID);

		var createdCourse = courseService.create(dto);

		assertNotNull(createdCourse.getId());
		assertEquals(CODE_ALG_UPPER, createdCourse.getCode());
		assertEquals(COURSE_ALG.trim(), createdCourse.getName());
		assertEquals(
				TEST_TEACHER_ID,
				createdCourse.getTeacher().getId());
	}

	@Test
	@DisplayName("create: invalid code pattern (DTO) -> ConstraintViolationException")
	void create_badCode_fails() {
		var dto = new CourseCreateDto(
				BAD_CODE,
				COURSE_ALG,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.create(dto));
	}

	@Test
	@DisplayName("create: null teacherId in DTO -> ConstraintViolationException")
	void create_nullTeacher_fails() {
		var dto = new CourseCreateDto(
				CODE_ALG_UPPER,
				COURSE_SECURITY,
				null,
				null);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.create(dto));
	}

	@Test
	@DisplayName("create: code already exists in DB (case-insensitive) -> CourseCreateException")
	void create_codeConflictInDb_fails() {
		var dto = new CourseCreateDto(
				CODE_SEC_UPPER,
				COURSE_DATABASES,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(dto));
	}

	@Test
	@DisplayName("create: name already exists in DB (case-insensitive) -> CourseCreateException")
	void create_nameConflictInDb_fails() {
		var dto = new CourseCreateDto(
				CODE_DB_UPPER,
				COURSE_SECURITY,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(dto));
	}

	@Test
	@DisplayName("create: non-existing teacher id -> CourseCreateException")
	void create_missingTeacher_fails() {
		var dto = new CourseCreateDto(
				CODE_ALG_UPPER,
				COURSE_ALG,
				null,
				NON_EXISTENT_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(dto));
	}
}