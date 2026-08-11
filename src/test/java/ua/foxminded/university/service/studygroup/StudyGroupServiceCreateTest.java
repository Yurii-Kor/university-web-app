package ua.foxminded.university.service.studygroup;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.*;

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
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;

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
class StudyGroupServiceCreateTest {

	@Autowired
	private StudyGroupService groupService;

	@Test
	@DisplayName("create: happy path — creates study group")
	void create_happyPath_success() {
		var dto = new StudyGroupCreateDto(
				GROUP_CREATE);

		var created = groupService.create(dto);

		assertNotNull(created.getId());

		assertEquals(
				GROUP_CREATE,
				created.getName());
	}

	@Test
	@DisplayName("create: invalid name -> ConstraintViolationException")
	void create_invalidName_fails() {
		var dto = new StudyGroupCreateDto(
				BAD_NAME);

		assertThrows(
				ConstraintViolationException.class,
				() -> groupService.create(dto));
	}

	@Test
	@DisplayName("create: duplicate name -> IllegalArgumentException")
	void create_duplicateName_fails() {
		var dto = new StudyGroupCreateDto(
				GROUP_CS);

		assertThrows(
				IllegalArgumentException.class,
				() -> groupService.create(dto));
	}
}