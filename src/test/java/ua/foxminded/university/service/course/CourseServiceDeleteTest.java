package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

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
class CourseServiceDeleteTest {

	@Autowired
	private CourseService courseService;

	@Test
	@DisplayName("delete: existing id -> deletes course")
	void delete_existingId_deletes() {
		var courseToDelete = courseService.create(
				new CourseCreateDto(
						TO_DELETE_CODE,
						TO_DELETE_NAME,
						null,
						TEST_TEACHER_ID));

		Long id = courseToDelete.getId();

		assertNotNull(id);

		courseService.delete(id);

		assertTrue(
				courseService.findByIds(List.of(id)).isEmpty(),
				"course must be deleted");
	}

	@Test
	@DisplayName("delete: non-existing id -> EntityNotFoundException")
	void delete_notFound_throwsEntityNotFound() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.delete(NON_EXISTENT_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}
}