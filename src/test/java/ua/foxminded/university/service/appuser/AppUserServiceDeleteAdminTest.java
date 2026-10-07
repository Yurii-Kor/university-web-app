package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.appuser.AppUserServiceTestConstants.*;

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
class AppUserServiceDeleteAdminTest {

	@Autowired
	private AppUserService appUserService;

	@Test
	@DisplayName("deleteAdmin: non-admin user -> IllegalStateException")
	void deleteAdmin_student_guard_fails() {
		assertThrows(
				IllegalStateException.class,
				() -> appUserService.deleteAdmin(
						STUDENT_USER_ID));
	}

	@Test
	@DisplayName("deleteAdmin: guard — cannot delete last admin")
	void deleteAdmin_lastAdmin_guard_fails() {
		assertThrows(
				IllegalStateException.class,
				() -> appUserService.deleteAdmin(
						TEST_ADMIN_ID));
	}

	@Test
	@DisplayName("deleteAdmin: missing id -> EntityNotFoundException")
	void deleteAdmin_missingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.deleteAdmin(
						MISSING_ID));
	}
}