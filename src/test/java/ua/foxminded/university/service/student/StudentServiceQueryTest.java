package ua.foxminded.university.service.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.student.StudentServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;

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
class StudentServiceQueryTest {

	@Autowired
	private StudentService studentService;

	@Test
	@DisplayName("listStudentCardsForAccountManagement: returns active students including disabled")
	void listStudentCardsForAccountManagement_returnsActiveStudentsIncludingDisabled() {
		var page = studentService.listStudentCardsForAccountManagement(
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertEquals(
				ACTIVE_STUDENT_COUNT,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(STUDENT_BLOCKED_ID),
				"disabled student must remain visible for account management");

		assertFalse(
				ids.contains(STUDENT_DELETED_ID),
				"soft-deleted student must not be visible");

		var blockedCard = page.getContent()
				.stream()
				.filter(card -> card.id().equals(
						STUDENT_BLOCKED_ID))
				.findFirst()
				.orElseThrow();

		assertFalse(
				blockedCard.enabled());
	}

	@Test
	@DisplayName("listStudentCardsForAdmin: returns only enabled students")
	void listStudentCardsForAdmin_returnsEnabledStudentsOnly() {
		var page = studentService.listStudentCardsForAdmin(
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertEquals(
				ACTIVE_ENABLED_STUDENT_COUNT,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertFalse(
				ids.contains(STUDENT_BLOCKED_ID));

		assertFalse(
				ids.contains(STUDENT_DELETED_ID));

		var card = page.getContent()
				.stream()
				.filter(student -> student.id().equals(
						STUDENT_1_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				STUDENT_1_EMAIL,
				card.email());

		assertEquals(
				STUDENT_1_FIRST_NAME,
				card.firstName());

		assertEquals(
				STUDENT_1_LAST_NAME,
				card.lastName());

		assertEquals(
				GROUP_CS_ID,
				card.groupId());

		assertEquals(
				GROUP_CS,
				card.groupName());

		assertEquals(
				STUDENT_1_ENROLLMENT_YEAR,
				card.enrollmentYear());
	}

	@Test
	@DisplayName("listStudentCardsForTeacher: returns enabled students from teacher groups without duplicates")
	void listStudentCardsForTeacher_returnsTeacherStudentsWithoutDuplicates() {
		var page = studentService.listStudentCardsForTeacher(
				TEACHER_1_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertEquals(
				TEACHER_1_STUDENT_COUNT,
				page.getTotalElements(),
				"multiple teacher courses in one group must not duplicate students");

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.containsAll(
						List.of(
								STUDENT_1_ID,
								STUDENT_2_ID,
								STUDENT_3_ID)));

		assertFalse(
				ids.contains(STUDENT_BLOCKED_ID));

		assertFalse(
				ids.contains(STUDENT_DELETED_ID));

		assertFalse(
				ids.contains(STUDENT_4_ID));

		var student = page.getContent()
				.stream()
				.filter(card -> card.id().equals(
						STUDENT_1_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				GROUP_CS,
				student.groupName());
	}

	@Test
	@DisplayName("listStudentCardsForTeacher: second teacher returns students from own groups")
	void listStudentCardsForTeacher_secondTeacher_returnsOwnStudents() {
		var page = studentService.listStudentCardsForTeacher(
				TEACHER_2_ID,
				PageRequest.of(0, 10));

		assertEquals(
				ONE_STUDENT,
				page.getTotalElements());

		var student = page.getContent()
				.getFirst();

		assertEquals(
				STUDENT_4_ID,
				student.id());

		assertEquals(
				GROUP_EE,
				student.groupName());
	}

	@Test
	@DisplayName("listStudentCardsForTeacher: missing teacher returns empty page")
	void listStudentCardsForTeacher_missingTeacher_returnsEmpty() {
		var page = studentService.listStudentCardsForTeacher(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertTrue(
				page.isEmpty());
	}

	@Test
	@DisplayName("listStudentCardsForStudent: returns enabled groupmates excluding self")
	void listStudentCardsForStudent_returnsEnabledGroupmatesExcludingSelf() {
		var page = studentService.listStudentCardsForStudent(
				STUDENT_1_ID,
				PageRequest.of(0, 10));

		assertEquals(
				ONE_STUDENT,
				page.getTotalElements());

		var peer = page.getContent()
				.getFirst();

		assertEquals(
				STUDENT_2_ID,
				peer.id());

		assertEquals(
				STUDENT_2_EMAIL,
				peer.email());
	}

	@Test
	@DisplayName("listStudentCardsForStudent: missing student returns empty page")
	void listStudentCardsForStudent_missingStudent_returnsEmpty() {
		var page = studentService.listStudentCardsForStudent(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertTrue(
				page.isEmpty());
	}

	@Test
	@DisplayName("getStudentProfileView: existing student returns profile")
	void getStudentProfileView_existingStudent_returnsProfile() {
		var profile = studentService.getStudentProfileView(
				STUDENT_1_ID);

		assertNotNull(profile);

		assertEquals(
				STUDENT_1_EMAIL,
				profile.email());

		assertEquals(
				STUDENT_1_FIRST_NAME,
				profile.firstName());

		assertEquals(
				STUDENT_1_LAST_NAME,
				profile.lastName());

		assertEquals(
				STUDENT_1_ENROLLMENT_YEAR,
				profile.enrollmentYear());

		assertEquals(
				GROUP_CS,
				profile.groupName());

		assertNotNull(
				profile.createdAt());
	}

	@Test
	@DisplayName("getStudentProfileView: missing student -> EntityNotFoundException")
	void getStudentProfileView_missingStudent_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> studentService.getStudentProfileView(
						MISSING_ID));

		assertEquals(
				"Student not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}
}