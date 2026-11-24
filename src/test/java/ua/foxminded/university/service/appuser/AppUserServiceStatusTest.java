package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.appuser.AppUserServiceTestConstants.*;

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

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	scripts = "/sql/appuser/app-user-service-base.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class AppUserServiceStatusTest {

	@Autowired
	private AppUserService appUserService;

	@Test
	@DisplayName("enableUserByIds: contains missing id -> EntityNotFoundException")
	void enableUserByIds_containsMissingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.enableUserByIds(
						MISSING_ID));
	}

	@Test
	@DisplayName("disableUserByIds: can disable non-admin user")
	void disableUserByIds_student_ok() {
		appUserService.disableUserByIds(
				STUDENT_USER_ID);

		var reloaded = appUserService.findByIds(
				List.of(STUDENT_USER_ID))
				.getFirst();

		assertFalse(
				reloaded.isEnabled(),
				"Student must become disabled");
	}

	@Test
	@DisplayName("enableUserByIds: can enable non-admin user")
	void enableUserByIds_student_ok() {
		appUserService.enableUserByIds(
				STUDENT_USER_ID);

		var reloaded = appUserService.findByIds(
				List.of(STUDENT_USER_ID))
				.getFirst();

		assertTrue(
				reloaded.isEnabled(),
				"Student must remain enabled");
	}

	@Test
	@DisplayName("disableUserByIds: guard — cannot disable the last enabled admin")
	void disableUserByIds_lastEnabledAdmin_guard_fails() {
		assertThrows(
				IllegalStateException.class,
				() -> appUserService.disableUserByIds(
						TEST_ADMIN_ID),
				"must fail when trying to disable the last enabled admin");
	}

	@Test
	@DisplayName("disableUserByIds: contains missing id -> EntityNotFoundException")
	void disableUserByIds_containsMissingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.disableUserByIds(
						MISSING_ID));
	}
}