package ua.foxminded.university.service.studygroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.GROUP_CS;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.GROUP_EE;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.GROUP_EE_ID;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.GROUP_RENAMED;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.MISSING_ID;

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
import ua.foxminded.university.service.studygroup.dto.StudyGroupRenameDto;
import ua.foxminded.university.service.studygroup.exception.StudyGroupRenameException;

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
class StudyGroupServiceRenameTest {

	@Autowired
	private StudyGroupService groupService;

	@Test
	@DisplayName("rename: happy path — updates group name")
	void rename_happyPath_success() {
		var dto = new StudyGroupRenameDto(
				GROUP_RENAMED);

		var renamed = groupService.rename(
				GROUP_EE_ID,
				dto);

		assertEquals(
				GROUP_EE_ID,
				renamed.getId());

		assertEquals(
				GROUP_RENAMED,
				renamed.getName());
	}

	@Test
	@DisplayName("rename: missing group -> EntityNotFoundException")
	void rename_missingGroup_fails() {
		var dto = new StudyGroupRenameDto(
				GROUP_RENAMED);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> groupService.rename(
						MISSING_ID,
						dto));

		assertEquals(
				"StudyGroup not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("rename: existing name -> StudyGroupRenameException")
	void rename_existingName_fails() {
		var dto = new StudyGroupRenameDto(
				GROUP_CS);

		var exception = assertThrows(
				StudyGroupRenameException.class,
				() -> groupService.rename(
						GROUP_EE_ID,
						dto));

		assertEquals(
				GROUP_EE_ID,
				exception.getGroupId());

		assertEquals(
				GROUP_CS,
				exception.getRequestedName());

		assertEquals(
				"Study group id=%d cannot be renamed: name '%s' already exists"
						.formatted(
								GROUP_EE_ID,
								GROUP_CS),
				exception.getMessage());
	}

	@Test
	@DisplayName("rename: same name -> no-op")
	void rename_sameName_noop() {
		var dto = new StudyGroupRenameDto(
				GROUP_EE);

		var renamed = groupService.rename(
				GROUP_EE_ID,
				dto);

		assertEquals(
				GROUP_EE_ID,
				renamed.getId());

		assertEquals(
				GROUP_EE,
				renamed.getName());
	}
}