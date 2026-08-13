package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
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
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.service.course.exception.CourseSelfUpdateException;

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
class CourseServiceUpdateSelfTest {

	@Autowired
	private CourseService courseService;

	@Test
	@DisplayName("updateSelf: happy path — updates code & name")
	void updateSelf_happyPath_success() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				CODE_DB_UPPER,
				COURSE_DATABASES,
				null);

		courseService.updateSelf(dto);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(CODE_DB_UPPER, reloaded.getCode());
		assertEquals(COURSE_DATABASES, reloaded.getName());

		assertEquals(
				TEST_TEACHER_ID,
				reloaded.getTeacher().getId(),
				"teacher must stay unchanged when teacherId is null");
	}

	@Test
	@DisplayName("updateSelf: change teacher -> ok")
	void updateSelf_changeTeacher_success() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				null,
				null,
				OTHER_TEACHER_ID);

		courseService.updateSelf(dto);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				OTHER_TEACHER_ID,
				reloaded.getTeacher().getId());

		assertEquals(
				CODE_SEC_UPPER,
				reloaded.getCode(),
				"code must remain unchanged");

		assertEquals(
				COURSE_SECURITY,
				reloaded.getName(),
				"name must remain unchanged");
	}

	@Test
	@DisplayName("updateSelf: change teacher to non-existing -> CourseSelfUpdateException")
	void updateSelf_changeTeacherToMissing_fails() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				null,
				null,
				NON_EXISTENT_ID);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: teacherId = 0 -> ConstraintViolationException (@Positive)")
	void updateSelf_teacherIdZero_fails() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				null,
				null,
				0L);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: null id in DTO -> ConstraintViolationException")
	void updateSelf_nullId_fails() {
		var dto = new CourseSelfUpdateDto(
				null,
				null,
				COURSE_SECURITY,
				null);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: non-existing id -> EntityNotFoundException")
	void updateSelf_notFound_fails() {
		var dto = new CourseSelfUpdateDto(
				NON_EXISTENT_ID,
				null,
				COURSE_SECURITY,
				null);

		assertThrows(
				EntityNotFoundException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: no-op code (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopCode_ok() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				CODE_SEC_UPPER,
				null,
				null);

		var updated = courseService.updateSelf(dto);

		assertEquals(
				CODE_SEC_UPPER,
				updated.getCode());
	}

	@Test
	@DisplayName("updateSelf: change code to existing in DB -> CourseSelfUpdateException")
	void updateSelf_changeCodeToExisting_fails() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				CODE_OS_UPPER,
				null,
				null);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: bad code pattern -> ConstraintViolationException")
	void updateSelf_badCodePattern_fails() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				BAD_CODE,
				null,
				null);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(dto));
	}

	@Test
	@DisplayName("updateSelf: no-op name (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopName_ok() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				null,
				COURSE_SECURITY_LOWER,
				null);

		var updated = courseService.updateSelf(dto);

		assertEquals(
				COURSE_SECURITY,
				updated.getName());
	}

	@Test
	@DisplayName("updateSelf: change name to existing (case-insensitive) -> CourseSelfUpdateException")
	void updateSelf_changeNameToExisting_fails() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				null,
				COURSE_OS,
				null);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(dto));
	}
}