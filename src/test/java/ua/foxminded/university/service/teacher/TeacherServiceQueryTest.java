package ua.foxminded.university.service.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.teacher.TeacherServiceTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.TestcontainersConfiguration;

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
class TeacherServiceQueryTest {

	@Autowired
	private TeacherService teacherService;

	@Test
	@DisplayName("listTeacherCardsForAdmin: returns enabled active teachers")
	void listTeacherCardsForAdmin_returnsEnabledActiveTeachers() {
		var page = teacherService.listTeacherCardsForAdmin(
				PageRequest.of(
						0,
						10));

		assertNotNull(
				page);

		assertEquals(
				ACTIVE_ENABLED_TEACHER_COUNT,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.containsAll(
						List.of(
								TEACHER_1_ID,
								TEACHER_2_ID,
								TEACHER_3_ID)));

		assertFalse(
				ids.contains(
						TEACHER_BLOCKED_ID));

		assertFalse(
				ids.contains(
						TEACHER_DELETED_ID));

		var teacher = page.getContent()
				.stream()
				.filter(card ->
						card.id().equals(
								TEACHER_1_ID))
				.findFirst()
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						TEACHER_1_EMAIL,
						teacher.email()),
				() -> assertEquals(
						TEACHER_1_FIRST_NAME,
						teacher.firstName()),
				() -> assertEquals(
						TEACHER_1_LAST_NAME,
						teacher.lastName()),
				() -> assertEquals(
						TEACHER_1_RANK,
						teacher.academicRank()),
				() -> assertEquals(
						TEACHER_1_OFFICE,
						teacher.office()));
	}

	@Test
	@DisplayName("listTeacherCardsForTeacher: returns enabled colleagues excluding self")
	void listTeacherCardsForTeacher_returnsColleaguesExcludingSelf() {
		var page = teacherService.listTeacherCardsForTeacher(
				TEACHER_1_ID,
				PageRequest.of(
						0,
						10));

		assertEquals(
				TEACHER_1_DIRECTORY_COUNT,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.containsAll(
						List.of(
								TEACHER_2_ID,
								TEACHER_3_ID)));

		assertFalse(
				ids.contains(
						TEACHER_1_ID),
				"current teacher must not see their own card");

		assertFalse(
				ids.contains(
						TEACHER_BLOCKED_ID));

		assertFalse(
				ids.contains(
						TEACHER_DELETED_ID));
	}

	@Test
	@DisplayName("listTeacherCardsForTeacher: missing teacher id still excludes no existing teacher")
	void listTeacherCardsForTeacher_missingTeacher_returnsEnabledTeachers() {
		var page = teacherService.listTeacherCardsForTeacher(
				MISSING_ID,
				PageRequest.of(
						0,
						10));

		assertEquals(
				ACTIVE_ENABLED_TEACHER_COUNT,
				page.getTotalElements());
	}

	@Test
	@DisplayName("listTeacherCardsForStudent: returns enabled teachers for student group without duplicates")
	void listTeacherCardsForStudent_returnsGroupTeachersWithoutDuplicates() {
		var page = teacherService.listTeacherCardsForStudent(
				STUDENT_CS_ID,
				PageRequest.of(
						0,
						10));

		assertEquals(
				ONE_TEACHER,
				page.getTotalElements(),
				"multiple courses taught by one teacher must not duplicate teacher cards");

		var teacher = page.getContent()
				.getFirst();

		assertAll(
				() -> assertEquals(
						TEACHER_1_ID,
						teacher.id()),
				() -> assertEquals(
						TEACHER_1_EMAIL,
						teacher.email()),
				() -> assertEquals(
						TEACHER_1_RANK,
						teacher.academicRank()),
				() -> assertEquals(
						TEACHER_1_OFFICE,
						teacher.office()));
	}

	@Test
	@DisplayName("listTeacherCardsForStudent: second group returns own teacher")
	void listTeacherCardsForStudent_secondGroup_returnsOwnTeacher() {
		var page = teacherService.listTeacherCardsForStudent(
				STUDENT_EE_ID,
				PageRequest.of(
						0,
						10));

		assertEquals(
				ONE_TEACHER,
				page.getTotalElements());

		assertEquals(
				TEACHER_2_ID,
				page.getContent()
						.getFirst()
						.id());
	}

	@Test
	@DisplayName("listTeacherCardsForStudent: missing student returns empty page")
	void listTeacherCardsForStudent_missingStudent_returnsEmpty() {
		var page = teacherService.listTeacherCardsForStudent(
				MISSING_ID,
				PageRequest.of(
						0,
						10));

		assertNotNull(
				page);

		assertTrue(
				page.isEmpty());
	}

	@Test
	@DisplayName("listTeacherRoleChangeCardsForAdmin: returns active teachers including disabled")
	void listTeacherRoleChangeCardsForAdmin_returnsActiveTeachersIncludingDisabled() {
		var page = teacherService.listTeacherRoleChangeCardsForAdmin(
				PageRequest.of(
						0,
						10));

		assertEquals(
				ACTIVE_TEACHER_COUNT,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(
						TEACHER_BLOCKED_ID),
				"disabled teacher must remain visible for account management");

		assertFalse(
				ids.contains(
						TEACHER_DELETED_ID));

		var blocked = page.getContent()
				.stream()
				.filter(card ->
						card.id().equals(
								TEACHER_BLOCKED_ID))
				.findFirst()
				.orElseThrow();

		assertFalse(
				blocked.enabled());
	}

	@Test
	@DisplayName("listDeletedTeacherCardsForAdmin: returns soft-deleted teachers")
	void listDeletedTeacherCardsForAdmin_returnsDeletedTeachers() {
		var page = teacherService.listDeletedTeacherCardsForAdmin(
				PageRequest.of(
						0,
						10));

		assertEquals(
				ONE_TEACHER,
				page.getTotalElements());

		var teacher = page.getContent()
				.getFirst();

		assertAll(
				() -> assertEquals(
						TEACHER_DELETED_ID,
						teacher.id()),
				() -> assertEquals(
						TEACHER_DELETED_EMAIL,
						teacher.email()),
				() -> assertNotNull(
						teacher.deletedAt()));
	}

	@Test
	@DisplayName("listTeacherOptions: returns active teachers")
	void listTeacherOptions_returnsActiveTeachers() {
		var teachers = teacherService.listTeacherOptions();

		assertEquals(
				ACTIVE_TEACHER_COUNT,
				teachers.size());

		var ids = teachers.stream()
				.map(teacher -> teacher.id())
				.toList();

		assertTrue(
				ids.contains(
						TEACHER_BLOCKED_ID));

		assertFalse(
				ids.contains(
						TEACHER_DELETED_ID));
	}

	@Test
	@DisplayName("getTeacherProfileView: existing teacher returns profile")
	void getTeacherProfileView_existingTeacher_returnsProfile() {
		var profile = teacherService.getTeacherProfileView(
				TEACHER_1_ID);

		assertNotNull(
				profile);

		assertAll(
				() -> assertEquals(
						TEACHER_1_EMAIL,
						profile.email()),
				() -> assertEquals(
						TEACHER_1_FIRST_NAME,
						profile.firstName()),
				() -> assertEquals(
						TEACHER_1_LAST_NAME,
						profile.lastName()),
				() -> assertEquals(
						TEACHER_1_RANK,
						profile.academicRank()),
				() -> assertEquals(
						TEACHER_1_OFFICE,
						profile.office()),
				() -> assertNotNull(
						profile.createdAt()));
	}

	@Test
	@DisplayName("getTeacherProfileView: missing teacher -> EntityNotFoundException")
	void getTeacherProfileView_missingTeacher_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> teacherService.getTeacherProfileView(
						MISSING_ID));

		assertEquals(
				"Teacher not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}
}