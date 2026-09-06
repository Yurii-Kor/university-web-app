package ua.foxminded.university.service.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.teacher.TeacherServiceTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.service.teacher.dto.TeacherUpdateDto;

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
class TeacherServiceUpdateTest {

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TeacherRepository teacherRepository;

	@Test
	@DisplayName("update: happy path — updates academic rank and office")
	void update_happyPath_updatesRankAndOffice() {
		var dto = new TeacherUpdateDto(
				UPDATED_RANK,
				UPDATED_OFFICE);

		teacherService.update(
				TEACHER_3_ID,
				dto);

		var teacher = teacherRepository.findById(
				TEACHER_3_ID)
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						UPDATED_RANK,
						teacher.getAcademicRank()),
				() -> assertEquals(
						UPDATED_OFFICE,
						teacher.getOffice()));
	}

	@Test
	@DisplayName("update: academic rank changed — updates academic rank only")
	void update_rankChanged_updatesRankOnly() {
		var dto = new TeacherUpdateDto(
				UPDATED_RANK,
				null);

		teacherService.update(
				TEACHER_3_ID,
				dto);

		var teacher = teacherRepository.findById(
				TEACHER_3_ID)
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						UPDATED_RANK,
						teacher.getAcademicRank()),
				() -> assertEquals(
						TEACHER_3_OFFICE,
						teacher.getOffice()));
	}

	@Test
	@DisplayName("update: office changed — updates office only")
	void update_officeChanged_updatesOfficeOnly() {
		var dto = new TeacherUpdateDto(
				null,
				UPDATED_OFFICE);

		teacherService.update(
				TEACHER_3_ID,
				dto);

		var teacher = teacherRepository.findById(
				TEACHER_3_ID)
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						TEACHER_3_RANK,
						teacher.getAcademicRank()),
				() -> assertEquals(
						UPDATED_OFFICE,
						teacher.getOffice()));
	}

	@Test
	@DisplayName("update: empty patch — leaves teacher unchanged")
	void update_emptyPatch_keepsTeacherUnchanged() {
		var dto = new TeacherUpdateDto(
				null,
				null);

		teacherService.update(
				TEACHER_3_ID,
				dto);

		var teacher = teacherRepository.findById(
				TEACHER_3_ID)
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						TEACHER_3_RANK,
						teacher.getAcademicRank()),
				() -> assertEquals(
						TEACHER_3_OFFICE,
						teacher.getOffice()));
	}

	@Test
	@DisplayName("update: missing teacher -> EntityNotFoundException")
	void update_missingTeacher_fails() {
		var dto = new TeacherUpdateDto(
				UPDATED_RANK,
				UPDATED_OFFICE);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> teacherService.update(
						MISSING_ID,
						dto));

		assertEquals(
				"Teacher not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}
}