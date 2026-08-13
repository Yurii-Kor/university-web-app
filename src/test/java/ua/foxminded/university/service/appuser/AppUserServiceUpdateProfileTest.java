package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.appuser.AppUserServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
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
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.appuser.dto.AppUserSelfUpdateDto;

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
class AppUserServiceUpdateProfileTest {

	@Autowired
	private AppUserService appUserService;

	@Test
	@DisplayName("updateProfileFields: happy path — email + first/last name updated")
	void updateProfileFields_happyPath_success() {
		var createDto = new AppUserCreateDto(
				ADMIN_UPDATE_EMAIL,
				PWD,
				"Alice",
				"Admin");

		var createdAdmin = appUserService.createAdmin(
				createDto);

		var updateDto = new AppUserSelfUpdateDto(
				createdAdmin.getId(),
				ADMIN_UPDATED_EMAIL,
				UPDATED_FIRST_NAME,
				UPDATED_LAST_NAME);

		appUserService.updateProfileFields(updateDto);

		var reloaded = appUserService.findByIds(
				List.of(createdAdmin.getId()))
				.getFirst();

		assertEquals(
				ADMIN_UPDATED_EMAIL,
				reloaded.getEmail());

		assertEquals(
				UPDATED_FIRST_NAME,
				reloaded.getFirstName());

		assertEquals(
				UPDATED_LAST_NAME,
				reloaded.getLastName());
	}

	@Test
	@DisplayName("updateProfileFields: no-op — same email and null names")
	void updateProfileFields_noop_sameEmail_ok() {
		var dto = new AppUserSelfUpdateDto(
				TEST_ADMIN_ID,
				TEST_ADMIN_EMAIL,
				null,
				null);

		appUserService.updateProfileFields(dto);

		var found = appUserService.findByIds(
				List.of(TEST_ADMIN_ID))
				.getFirst();

		assertEquals(
				TEST_ADMIN_EMAIL,
				found.getEmail(),
				"Email must remain unchanged");

		assertEquals(
				FIRST_NAME,
				found.getFirstName(),
				"First name must remain unchanged");

		assertEquals(
				LAST_NAME,
				found.getLastName(),
				"Last name must remain unchanged");
	}

	@Test
	@DisplayName("updateProfileFields: null id -> ConstraintViolationException")
	void updateProfileFields_nullId_fails() {
		var dto = new AppUserSelfUpdateDto(
				null,
				UPDATED_EMAIL,
				UPDATED_FIRST_NAME,
				UPDATED_LAST_NAME);

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.updateProfileFields(dto));
	}

	@Test
	@DisplayName("updateProfileFields: user not found -> EntityNotFoundException")
	void updateProfileFields_notFound_fails() {
		var dto = new AppUserSelfUpdateDto(
				MISSING_ID,
				UPDATED_EMAIL,
				null,
				null);

		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.updateProfileFields(dto));
	}

	@Test
	@DisplayName("updateProfileFields: set email that belongs to another user -> IllegalArgumentException")
	void updateProfileFields_emailTaken_fails() {
		var dto = new AppUserSelfUpdateDto(
				TEST_ADMIN_ID,
				TAKEN_EMAIL,
				null,
				null);

		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.updateProfileFields(dto));
	}
}