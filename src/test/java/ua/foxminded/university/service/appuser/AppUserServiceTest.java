package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

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
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;
import ua.foxminded.university.service.appuser.dto.AppUserSelfUpdateDto;
import ua.foxminded.university.service.appuser.exception.AdminCreateException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	statements = {
		"""
		insert into app_user (id, email, password, role, first_name, last_name, enabled)
		values (
			1001,
			'admin.test@example.com',
			'$2a$10$zuVKu2I7XCxtiSIIJIcWUurJKhS9FMsanCH50hsN4paQ2VzFqnpNm',
			'ADMIN',
			'Alice',
			'Cooper',
			true
		);
		""",
		"""
		insert into app_user (id, email, password, role, first_name, last_name, enabled)
		values (
			1002,
			'student.taken@example.com',
			'$2a$10$zuVKu2I7XCxtiSIIJIcWUurJKhS9FMsanCH50hsN4paQ2VzFqnpNm',
			'STUDENT',
			'Bob',
			'User',
			true
		);
		"""
	},
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class AppUserServiceTest {

	static final Long TEST_ADMIN_ID = 1001L;
	static final Long STUDENT_USER_ID = 1002L;
	static final Long MISSING_ID = 999_999L;

	static final String TEST_ADMIN_EMAIL = "admin.test@example.com";
	static final String TAKEN_EMAIL = "student.taken@example.com";
	static final String UPDATED_EMAIL = "admin.updated@example.com";
	static final String ADMIN_CREATE_EMAIL = "admin.create@example.com";
	static final String ADMIN_LIST_EMAIL = "admin.list@example.com";
	static final String ADMIN_UPDATE_EMAIL = "admin.update@example.com";
	static final String ADMIN_UPDATED_EMAIL = "admin.updated@example.com";
	static final String ADMIN_NULL_PASSWORD_EMAIL = "admin.null.password@example.com";

	static final String FIRST_NAME = "Alice";
	static final String LAST_NAME = "Cooper";

	static final String UPDATED_FIRST_NAME = "UpdatedFirstName";
	static final String UPDATED_LAST_NAME = "UpdatedLastName";

	static final String PWD = "Abcd1234!";
	static final String PWD_NEW = "Abcd1234!x";
	static final String PWD_BAD = "bad";

	static final Integer ONE_USER = 1;

	@Autowired
	PasswordPolicy passwordPolicy;

	@Autowired
	AppUserService appUserService;

	AppUserCreateDto newAdminDto(String email) {
		return new AppUserCreateDto(email, PWD, "Alice", "Admin");
	}

	AppUserSelfUpdateDto patchProfileDto(Long id, String email, String first, String last) {
		return new AppUserSelfUpdateDto(id, email, first, last);
	}

	AppUserPasswordChangeDto changePasswordDto(Long id, String current, String next) {
		return new AppUserPasswordChangeDto(id, current, next);
	}

	@Test
	@DisplayName("createAdmin: happy path — creates enabled ADMIN with encoded password")
	void createAdmin_happyPath_success() {
		var createdAdmin = appUserService.createAdmin(
				newAdminDto(ADMIN_CREATE_EMAIL));

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
		var bad = new AppUserCreateDto(
				null,
				PWD,
				"Alice",
				"Admin");

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.createAdmin(bad));
	}

	@Test
	@DisplayName("createAdmin: null password -> ConstraintViolationException")
	void createAdmin_nullPassword_fails() {
		var bad = new AppUserCreateDto(
				ADMIN_NULL_PASSWORD_EMAIL,
				null,
				"Alice",
				"Admin");

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.createAdmin(bad));
	}

	@Test
	@DisplayName("createAdmin: duplicate email ignoring case -> AdminCreateException")
	void createAdmin_duplicateEmailIgnoreCase_fails() {
		var duplicate = newAdminDto(
				TEST_ADMIN_EMAIL.toUpperCase());

		assertThrows(
				AdminCreateException.class,
				() -> appUserService.createAdmin(duplicate));
	}

	@Test
	@DisplayName("listAdmins: returns paged admin rows only")
	void listAdmins_returnsAdminsOnly() {
		var createdAdmin = appUserService.createAdmin(
				newAdminDto(ADMIN_LIST_EMAIL));

		var adminsPage = appUserService.listAdminsForView(
				createdAdmin.getId(),
				PageRequest.of(0, 10));

		var admins = adminsPage.getContent();

		assertNotNull(adminsPage);
		assertNotNull(admins);
		assertEquals(2, adminsPage.getTotalElements());
		assertEquals(1, adminsPage.getTotalPages());

		var adminEmails = admins.stream()
				.map(admin -> admin.email())
				.toList();

		assertTrue(adminEmails.contains(TEST_ADMIN_EMAIL));
		assertTrue(adminEmails.contains(ADMIN_LIST_EMAIL));
		assertFalse(adminEmails.contains(TAKEN_EMAIL));
	}

	@Test
	@DisplayName("getAdminProfileView: student id -> EntityNotFoundException")
	void getAdminProfileView_studentId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.getAdminProfileView(STUDENT_USER_ID));
	}

	@Test
	@DisplayName("updateProfileFields: happy path — email + first/last name updated")
	void updateProfileFields_happyPath_success() {
		var createdAdmin = appUserService.createAdmin(
				newAdminDto(ADMIN_UPDATE_EMAIL));

		appUserService.updateProfileFields(
				patchProfileDto(
						createdAdmin.getId(),
						ADMIN_UPDATED_EMAIL,
						UPDATED_FIRST_NAME,
						UPDATED_LAST_NAME));

		var reloaded = appUserService.findByIds(
				List.of(createdAdmin.getId()))
				.getFirst();

		assertEquals(ADMIN_UPDATED_EMAIL, reloaded.getEmail());
		assertEquals(UPDATED_FIRST_NAME, reloaded.getFirstName());
		assertEquals(UPDATED_LAST_NAME, reloaded.getLastName());
	}

	@Test
	@DisplayName("updateProfileFields: no-op — same email and null names")
	void updateProfileFields_noop_sameEmail_ok() {
		var patch = patchProfileDto(
				TEST_ADMIN_ID,
				TEST_ADMIN_EMAIL,
				null,
				null);

		appUserService.updateProfileFields(patch);

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
		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.updateProfileFields(
						patchProfileDto(
								null,
								UPDATED_EMAIL,
								UPDATED_FIRST_NAME,
								UPDATED_LAST_NAME)));
	}

	@Test
	@DisplayName("updateProfileFields: user not found -> EntityNotFoundException")
	void updateProfileFields_notFound_fails() {
		var patchMissing = patchProfileDto(
				MISSING_ID,
				UPDATED_EMAIL,
				null,
				null);

		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.updateProfileFields(patchMissing));
	}

	@Test
	@DisplayName("updateProfileFields: set email that belongs to another user -> IllegalArgumentException")
	void updateProfileFields_emailTaken_fails() {
		var patchWithTakenEmail = patchProfileDto(
				TEST_ADMIN_ID,
				TAKEN_EMAIL,
				null,
				null);

		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.updateProfileFields(patchWithTakenEmail));
	}

	@Test
	@DisplayName("enableUserByIds: contains missing id -> EntityNotFoundException")
	void enableUserByIds_containsMissingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.enableUserByIds(MISSING_ID));
	}

	@Test
	@DisplayName("disableUserByIds: can disable non-admin user")
	void disableUserByIds_student_ok() {
		appUserService.disableUserByIds(STUDENT_USER_ID);

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
		appUserService.enableUserByIds(STUDENT_USER_ID);

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
				() -> appUserService.disableUserByIds(TEST_ADMIN_ID),
				"must fail when trying to disable the last enabled admin");
	}

	@Test
	@DisplayName("disableUserByIds: contains missing id -> EntityNotFoundException")
	void disableUserByIds_containsMissingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.disableUserByIds(MISSING_ID));
	}

	@Test
	@DisplayName("changePasswordSelf: happy path — password changed and encoded")
	void changePasswordSelf_happyPath_success() {
		appUserService.changePasswordSelf(
				changePasswordDto(
						TEST_ADMIN_ID,
						PWD,
						PWD_NEW));

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
		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						new AppUserPasswordChangeDto(
								null,
								PWD,
								PWD_NEW)));

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								TEST_ADMIN_ID,
								null,
								PWD_NEW)));

		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								TEST_ADMIN_ID,
								PWD,
								null)));
	}

	@Test
	@DisplayName("changePasswordSelf: new password same as current -> IllegalArgumentException")
	void changePasswordSelf_samePassword_fails() {
		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								TEST_ADMIN_ID,
								PWD,
								PWD)));
	}

	@Test
	@DisplayName("changePasswordSelf: user not found -> EntityNotFoundException")
	void changePasswordSelf_notFound_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								MISSING_ID,
								PWD,
								PWD_NEW)));
	}

	@Test
	@DisplayName("changePasswordSelf: wrong current password -> IllegalArgumentException")
	void changePasswordSelf_wrongCurrent_fails() {
		assertThrows(
				IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								TEST_ADMIN_ID,
								PWD_NEW,
								PWD_NEW)));
	}

	@Test
	@DisplayName("changePasswordSelf: new password violates pattern -> ConstraintViolationException")
	void changePasswordSelf_badNewPassword_fails() {
		assertThrows(
				ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(
						changePasswordDto(
								TEST_ADMIN_ID,
								PWD,
								PWD_BAD)));
	}

	@Test
	@DisplayName("findByIds: null/empty/with null and duplicated id inside -> correct response")
	void findByIds_various() {
		assertTrue(appUserService.findByIds(null).isEmpty());
		assertTrue(appUserService.findByIds(List.of()).isEmpty());

		var found = appUserService.findByIds(
				Arrays.asList(
						null,
						TEST_ADMIN_ID,
						TEST_ADMIN_ID));

		assertEquals(ONE_USER, found.size());
		assertEquals(TEST_ADMIN_ID, found.getFirst().getId());
	}

	@Test
	@DisplayName("getAdminProfileView: happy path — returns AdminProfileView for existing ADMIN")
	void getAdminProfileView_happyPath_success() {
		var view = appUserService.getAdminProfileView(TEST_ADMIN_ID);

		assertNotNull(view);
		assertEquals(TEST_ADMIN_EMAIL, view.email());
		assertEquals(FIRST_NAME, view.firstName());
		assertEquals(LAST_NAME, view.lastName());
		assertNotNull(
				view.createdAt(),
				"createdAt must be populated by DB");
	}

	@Test
	@DisplayName("getAdminProfileView: missing id -> EntityNotFoundException")
	void getAdminProfileView_missingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.getAdminProfileView(MISSING_ID));
	}

	@Test
	@DisplayName("deleteAdmin: non-admin user -> IllegalStateException")
	void deleteAdmin_student_guard_fails() {
		assertThrows(
				IllegalStateException.class,
				() -> appUserService.deleteAdmin(STUDENT_USER_ID));
	}

	@Test
	@DisplayName("deleteAdmin: guard — cannot delete last admin")
	void deleteAdmin_lastAdmin_guard_fails() {
		assertThrows(
				IllegalStateException.class,
				() -> appUserService.deleteAdmin(TEST_ADMIN_ID));
	}

	@Test
	@DisplayName("deleteAdmin: missing id -> EntityNotFoundException")
	void deleteAdmin_missingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.deleteAdmin(MISSING_ID));
	}
}