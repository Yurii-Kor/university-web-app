package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.appuser.AppUserServiceTestConstants.*;

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
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.appuser.exception.AdminCreateException;

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
class AppUserServiceCreateAdminTest {

	@Autowired
	private AppUserService appUserService;

	@Autowired
	private PasswordPolicy passwordPolicy;

	@Test
	@DisplayName("createAdmin: happy path — creates enabled ADMIN with encoded password")
	void createAdmin_happyPath_success() {
		var dto = new AppUserCreateDto(
				ADMIN_CREATE_EMAIL,
				PWD,
				"Alice",
				"Admin");

		var createdAdmin = appUserService.createAdmin(dto);

		assertNotNull(createdAdmin.getId());
		assertEquals(ADMIN_CREATE_EMAIL, createdAdmin.getEmail());
		assertEquals(UserRole.ADMIN, createdAdmin.getRole());
		assertTrue(createdAdmin.isEnabled());

		passwordPolicy.assertCurrentMatches(
				PWD,
				createdAdmin.getPassword());
	}

	@Test
	@DisplayName("createAdmin: null email -> ConstraintViolationException")
	void createAdmin_nullEmail_fails() {
		var dto = new AppUserCreateDto(
				null,
				PWD,
				"Alice",
				"Admin");

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.createAdmin(dto));
	}

	@Test
	@DisplayName("createAdmin: null password -> ConstraintViolationException")
	void createAdmin_nullPassword_fails() {
		var dto = new AppUserCreateDto(
				ADMIN_NULL_PASSWORD_EMAIL,
				null,
				"Alice",
				"Admin");

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.createAdmin(dto));
	}

	@Test
	@DisplayName("createAdmin: duplicate email ignoring case -> AdminCreateException")
	void createAdmin_duplicateEmailIgnoreCase_fails() {
		var dto = new AppUserCreateDto(
				TEST_ADMIN_EMAIL.toUpperCase(),
				PWD,
				"Alice",
				"Admin");

		assertThrows(
				AdminCreateException.class,
				() -> appUserService.createAdmin(dto));
	}
}