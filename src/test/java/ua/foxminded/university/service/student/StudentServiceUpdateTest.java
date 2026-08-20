package ua.foxminded.university.service.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.student.StudentServiceTestConstants.*;

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
import ua.foxminded.university.model.persistence.student.StudentRepository;
import ua.foxminded.university.service.student.dto.StudentEnrollmentYearUpdateDto;
import ua.foxminded.university.service.student.dto.StudentGroupUpdateDto;
import ua.foxminded.university.service.student.exception.StudentUpdateException;

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
class StudentServiceUpdateTest {

	@Autowired
	private StudentService studentService;

	@Autowired
	private StudentRepository studentRepository;

	@Test
	@DisplayName("moveToGroup: happy path — updates student's group")
	void moveToGroup_happyPath_success() {
		var dto = new StudentGroupUpdateDto(
				GROUP_ME_ID);

		studentService.moveToGroup(
				STUDENT_1_ID,
				dto);

		var groupId = studentRepository.findGroupIdByStudentId(
				STUDENT_1_ID)
				.orElseThrow();

		assertEquals(
				GROUP_ME_ID,
				groupId);
	}

	@Test
	@DisplayName("moveToGroup: missing student -> EntityNotFoundException")
	void moveToGroup_missingStudent_fails() {
		var dto = new StudentGroupUpdateDto(
				GROUP_ME_ID);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> studentService.moveToGroup(
						MISSING_ID,
						dto));

		assertEquals(
				"Student not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("moveToGroup: missing target group -> StudentUpdateException")
	void moveToGroup_missingGroup_fails() {
		var dto = new StudentGroupUpdateDto(
				MISSING_ID);

		var exception = assertThrows(
				StudentUpdateException.class,
				() -> studentService.moveToGroup(
						STUDENT_1_ID,
						dto));

		assertEquals(
				STUDENT_1_ID,
				exception.getStudentId());

		assertEquals(
				"StudyGroup not found: id="
						+ MISSING_ID,
				exception.getMessage());

		var groupId = studentRepository.findGroupIdByStudentId(
				STUDENT_1_ID)
				.orElseThrow();

		assertEquals(
				GROUP_CS_ID,
				groupId,
				"student group must remain unchanged after failed update");
	}

	@Test
	@DisplayName("updateEnrollmentYear: happy path — updates enrollment year")
	void updateEnrollmentYear_happyPath_success() {
		var dto = new StudentEnrollmentYearUpdateDto(
				UPDATED_ENROLLMENT_YEAR);

		studentService.updateEnrollmentYear(
				STUDENT_1_ID,
				dto);

		var student = studentRepository.findById(
				STUDENT_1_ID)
				.orElseThrow();

		assertEquals(
				UPDATED_ENROLLMENT_YEAR,
				student.getEnrollmentYear());
	}

	@Test
	@DisplayName("updateEnrollmentYear: missing student -> EntityNotFoundException")
	void updateEnrollmentYear_missingStudent_fails() {
		var dto = new StudentEnrollmentYearUpdateDto(
				UPDATED_ENROLLMENT_YEAR);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> studentService.updateEnrollmentYear(
						MISSING_ID,
						dto));

		assertEquals(
				"Student not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}
}