package ua.foxminded.university.service.studygroup;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.*;

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
import ua.foxminded.university.service.studygroup.exception.StudyGroupDeleteException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	scripts = "/sql/studygroup/study-group-service-base.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class StudyGroupServiceDeleteTest {

	@Autowired
	private StudyGroupService groupService;

	@Test
	@DisplayName("delete: empty group -> soft deletes group")
	void delete_emptyGroup_success() {
		groupService.delete(
				GROUP_ME_ID);

		assertTrue(
				groupService.findByIds(
						List.of(GROUP_ME_ID))
						.isEmpty(),
				"soft-deleted group must no longer be visible");
	}

	@Test
	@DisplayName("delete: group with active students -> StudyGroupDeleteException")
	void delete_groupWithActiveStudents_fails() {
		var exception = assertThrows(
				StudyGroupDeleteException.class,
				() -> groupService.delete(
						GROUP_CS_ID));

		assertEquals(
				GROUP_CS_ID,
				exception.getGroupId());

		assertEquals(
				2L,
				exception.getStudentCount());

		assertEquals(
				"Cannot delete non-empty group: id=%d, students=%d"
						.formatted(
								GROUP_CS_ID,
								2L),
				exception.getMessage());
	}

	@Test
	@DisplayName("delete: group with only soft-deleted students -> succeeds")
	void delete_groupWithOnlySoftDeletedStudents_success() {
		groupService.delete(
				GROUP_SE_ID);

		assertTrue(
				groupService.findByIds(
						List.of(GROUP_SE_ID))
						.isEmpty(),
				"group with no active students must be soft-deleted");
	}

	@Test
	@DisplayName("delete: missing group -> EntityNotFoundException")
	void delete_missingGroup_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> groupService.delete(
						MISSING_ID));

		assertEquals(
				"StudyGroup not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}
}