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
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;

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
class AppUserServicePasswordTest {

	@Autowired
	private AppUserService appUserService;

	@Autowired
	private PasswordPolicy passwordPolicy;

	@Test
	@DisplayName("changePasswordSelf: happy path — password changed and encoded")
	void changePasswordSelf_happyPath_success() {
		var dto = new AppUserPasswordChangeDto(
				TEST_ADMIN_ID,
				PWD,
				PWD_NEW);

		appUserService.changePasswordSelf(dto);

		var updated = appUserService.findByIds(
				List.of(TEST_ADMIN_ID))
				.getFirst();

		passwordPolicy.assertCurrentMatches(
				PWD_NEW,
				updated.getPassword());
	}

	@Test
	@DisplayName("changePasswordSelf: missing required fields -> ConstraintViolationException")
	void changePasswordSelf_missingFields_fails() {
		var nullIdDto = new AppUserPasswordChangeDto(
				null,
				PWD,
				PWD_NEW);

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						nullIdDto));

		var nullCurrentPasswordDto =
				new AppUserPasswordChangeDto(
						TEST_ADMIN_ID,
						null,
						PWD_NEW);

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						nullCurrentPasswordDto));

		var nullNewPasswordDto =
				new AppUserPasswordChangeDto(
						TEST_ADMIN_ID,
						PWD,
						null);

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						nullNewPasswordDto));
	}

	@Test
	@DisplayName("changePasswordSelf: new password same as current -> IllegalArgumentException")
	void changePasswordSelf_samePassword_fails() {
		var dto = new AppUserPasswordChangeDto(
				TEST_ADMIN_ID,
				PWD,
				PWD);

		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(dto));
	}

	@Test
	@DisplayName("changePasswordSelf: user not found -> EntityNotFoundException")
	void changePasswordSelf_notFound_fails() {
		var dto = new AppUserPasswordChangeDto(
				MISSING_ID,
				PWD,
				PWD_NEW);

		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.changePasswordSelf(dto));
	}

	@Test
	@DisplayName("changePasswordSelf: wrong current password -> IllegalArgumentException")
	void changePasswordSelf_wrongCurrent_fails() {
		var dto = new AppUserPasswordChangeDto(
				TEST_ADMIN_ID,
				PWD_NEW,
				PWD_NEW);

		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(dto));
	}

	@Test
	@DisplayName("changePasswordSelf: new password violates pattern -> ConstraintViolationException")
	void changePasswordSelf_badNewPassword_fails() {
		var dto = new AppUserPasswordChangeDto(
				TEST_ADMIN_ID,
				PWD,
				PWD_BAD);

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(dto));
	}
}