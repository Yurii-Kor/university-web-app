package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.service.dto.DeleteResult;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, AppUserService.class, ValidatorConfig.class, EntityValidatior.class,
		PasswordPolicy.class, PasswordEncoderConfig.class, TestDataInitializer.class })
class AppUserServiceTest {

	private static final String DEFAULT_EMAIL = "admin@example.com";
	private static final String UPDATED_EMAIL = "admin.updated@example.com";

	private static final String TEST_ADMIN_EMAIL = "admin.test@example.com";
	private static final String TAKEN_EMAIL = "student.taken@example.com";

	private static final String UPDATED_FIRST_NAME = "UpdatedFirstName";
	private static final String UPDATED_LAST_NAME = "UpdatedLastName";

	private static final String PWD = "Abcd1234!";
	private static final String PWD_NEW = "Abcd1234!x";
	private static final String PWD_BAD = "bad";

	private static final Long MISSING_ID = 999_999L;
	private static final Integer ONE_USER = 1;

	@Autowired
	private PasswordPolicy passwordPolicy;

	@Autowired
	private AppUserService appUserService;

	@Autowired
	private TestDataInitializer initializer;

	private AppUser testAdmin, userStudent;

	@BeforeAll
	void setup() {
		testAdmin = initializer.persistAll(draftAdmin(TEST_ADMIN_EMAIL)).getFirst();
		userStudent = initializer.persistAll(AppUser.builder()
				.email(TAKEN_EMAIL)
				.password("Abcd1234!")
				.role(UserRole.STUDENT)
				.firstName("Bob")
				.lastName("User")
				.enabled(true)
				.build()).get(0);
	}

	private AppUser draftAdmin(String email) {
		return AppUser.builder()
				.email(email)
				.password(PWD)
				.role(UserRole.ADMIN)
				.firstName("Alice")
				.lastName("Admin")
				.enabled(true)
				.build();
	}

	@Test
	@DisplayName("happy path: createAdmins -> updateProfileFields -> changePasswordSelf -> deleteAdminsByIds (with notFound) -> last-admin guard")
	void happyPath_fullCycle_success() {
		var createdAll = assertDoesNotThrow(() -> appUserService.createAdmins(List.of(draftAdmin(DEFAULT_EMAIL))));
		assertEquals(ONE_USER, createdAll.size());
		var created = createdAll.getFirst();

		assertNotNull(created.getId());
		assertEquals(DEFAULT_EMAIL, created.getEmail());
		assertEquals(UserRole.ADMIN, created.getRole());
		assertTrue(created.isEnabled());
		assertDoesNotThrow(() -> passwordPolicy.assertCurrentMatches(PWD, created.getPassword()));

		var patch = AppUser.builder()
				.email(UPDATED_EMAIL)
				.firstName(UPDATED_FIRST_NAME)
				.lastName(UPDATED_LAST_NAME)
				.build();

		assertDoesNotThrow(() -> appUserService.updateProfileFields(created.getId(), patch));
		initializer.clear();
		var reloaded = appUserService.findByIds(List.of(created.getId())).getFirst();
		assertEquals(UPDATED_EMAIL, reloaded.getEmail());
		assertEquals(UPDATED_FIRST_NAME, reloaded.getFirstName());
		assertEquals(UPDATED_LAST_NAME, reloaded.getLastName());

		assertDoesNotThrow(() -> appUserService.changePasswordSelf(created.getId(), PWD, PWD_NEW));
		initializer.clear();
		var updatedPwd = appUserService.findByIds(List.of(created.getId())).getFirst();
		assertNotEquals(passwordPolicy.encodeForChange(PWD), updatedPwd.getPassword());
		assertDoesNotThrow(() -> passwordPolicy.assertCurrentMatches(PWD_NEW, updatedPwd.getPassword()));

		DeleteResult del = assertDoesNotThrow(
				() -> appUserService.deleteAdminsByIds(List.of(created.getId(), MISSING_ID)));
		assertEquals(List.of(created.getId()), del.deletedIds());
		assertEquals(List.of(MISSING_ID), del.notFoundIds());

		assertThrows(IllegalStateException.class, () -> appUserService.deleteAdminsByIds(List.of(testAdmin.getId())));
	}

	@Test
	@DisplayName("createAdmins: null/empty -> empty list")
	void createAdmins_nullOrEmpty_returnsEmpty() {
		assertTrue(appUserService.createAdmins(null).isEmpty());
		assertTrue(appUserService.createAdmins(List.of()).isEmpty());
	}

	@Test
	@DisplayName("createAdmins: null email -> IllegalArgumentException")
	void createAdmins_nullEmail_fails() {
		var bad = draftAdmin(null);
		assertThrows(IllegalArgumentException.class, () -> appUserService.createAdmins(List.of(bad)));
	}

	@Test
	@DisplayName("createAdmins: null password -> IllegalArgumentException")
	void createAdmins_nullPassword_fails() {
		var bad = draftAdmin(DEFAULT_EMAIL).toBuilder().password(null).build();
		assertThrows(IllegalArgumentException.class, () -> appUserService.createAdmins(List.of(bad)));
	}

	@Test
	@DisplayName("createAdmins: duplicate emails in one batch (case-insensitive) -> IllegalArgumentException")
	void createAdmins_duplicateEmailsInOneBatch_fails() {
		var dupLower = draftAdmin(DEFAULT_EMAIL);
		var dupUpper = draftAdmin(DEFAULT_EMAIL);
		assertThrows(IllegalArgumentException.class, () -> appUserService.createAdmins(List.of(dupLower, dupUpper)));
	}

	@Test
	@DisplayName("createAdmins: duplicate email of created user -> IllegalArgumentException")
	void createAdmins_duplicateAcrossCalls_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.createAdmins(List.of(draftAdmin(testAdmin.getEmail()))));
	}

	@Test
	@DisplayName("updateProfileFields: no-op — same email (trim/case-insensitive) and null names")
	void updateProfileFields_noop_sameEmail_ok() {
		var patch = AppUser.builder().email("  " + testAdmin.getEmail().toUpperCase() + "  ").build();
		assertDoesNotThrow(() -> appUserService.updateProfileFields(testAdmin.getId(), patch));

		initializer.clear();
		var found = appUserService.findByIds(List.of(testAdmin.getId())).getFirst();

		assertEquals(testAdmin.getEmail(), found.getEmail(), "Email must remain unchanged");
		assertEquals(testAdmin.getFirstName(), found.getFirstName(), "First name must remain unchanged");
		assertEquals(testAdmin.getLastName(), found.getLastName(), "Last name must remain unchanged");
	}

	@Test
	@DisplayName("updateProfileFields: null args -> IllegalArgumentException")
	void updateProfileFields_nullArgs_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.updateProfileFields(null, AppUser.builder().build()));

		assertThrows(IllegalArgumentException.class, () -> appUserService.updateProfileFields(testAdmin.getId(), null));
	}

	@Test
	@DisplayName("updateProfileFields: user not found -> EntityNotFoundException")
	void updateProfileFields_notFound_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> appUserService.updateProfileFields(MISSING_ID, AppUser.builder().email(UPDATED_EMAIL).build()));
	}

	@Test
	@DisplayName("updateProfileFields: set email that belongs to another user -> IllegalArgumentException")
	void updateProfileFields_emailTaken_fails() {
		var patchWithTakenEmail = AppUser.builder().email(TAKEN_EMAIL).build();
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.updateProfileFields(testAdmin.getId(), patchWithTakenEmail));
	}

	@Test
	@DisplayName("enableUsersByIds: contains missing id -> EntityNotFoundException")
	void enableUsersByIds_containsMissingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> appUserService.enableUsersByIds(List.of(MISSING_ID)));
	}

	@Test
	@DisplayName("disableUsersByIds: can disable non-admin user")
	void disableUsersByIds_student_ok() {
		assertDoesNotThrow(() -> appUserService.disableUsersByIds(List.of(userStudent.getId())));

		initializer.clear();
		var reloaded = appUserService.findByIds(List.of(userStudent.getId())).getFirst();
		assertFalse(reloaded.isEnabled(), "Student must become disabled");
	}

	@Test
	@DisplayName("disableUsersByIds: can disable non-admin user")
	void enableUsersByIds_student_ok() {
		assertDoesNotThrow(() -> appUserService.enableUsersByIds(List.of(userStudent.getId())));

		initializer.clear();
		var reloaded = appUserService.findByIds(List.of(userStudent.getId())).getFirst();
		assertTrue(reloaded.isEnabled(), "Student must become disabled");
	}

	@Test
	@DisplayName("enableUsersByIds: null/empty/only-null -> returns 0")
	void enableUsersByIds_nullEmptyOnlyNull_returnsZero() {
		assertEquals(0, appUserService.enableUsersByIds(null));
		assertEquals(0, appUserService.enableUsersByIds(List.of()));
		assertEquals(0, appUserService.enableUsersByIds(Arrays.asList(null, null)));
	}

	@Test
	@DisplayName("disableUsersByIds: guard — cannot disable the last enabled admin")
	void disableUsersByIds_lastEnabledAdmin_guard_fails() {
		assertThrows(IllegalStateException.class,
				() -> appUserService.disableUsersByIds(List.of(testAdmin.getId())),
				"must fail when trying to disable the last enabled admin");
	}

	@Test
	@DisplayName("disableUsersByIds: contains missing id -> EntityNotFoundException")
	void disableUsersByIds_containsMissingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> appUserService.disableUsersByIds(List.of(MISSING_ID)));
	}

	@Test
	@DisplayName("changePasswordSelf: null args -> IllegalArgumentException")
	void changePasswordSelf_nullArgs_fails() {
		assertThrows(IllegalArgumentException.class, () -> appUserService.changePasswordSelf(null, PWD, PWD_NEW));
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(testAdmin.getId(), null, PWD_NEW));
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(testAdmin.getId(), PWD, null));
	}

	@Test
	@DisplayName("changePasswordSelf: user not found -> EntityNotFoundException")
	void changePasswordSelf_notFound_fails() {
		assertThrows(EntityNotFoundException.class, () -> appUserService.changePasswordSelf(MISSING_ID, PWD, PWD_NEW));
	}

	@Test
	@DisplayName("changePasswordSelf: wrong current password -> IllegalArgumentException")
	void changePasswordSelf_wrongCurrent_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(testAdmin.getId(), PWD_NEW, PWD_NEW));
	}

	@Test
	@DisplayName("changePasswordSelf: new pass doesn't match to filter -> ConstraintViolationException")
	void changePasswordSelf_badNewPassword_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(testAdmin.getId(), PWD, PWD_BAD));
	}

	@Test
	@DisplayName("findByIds: null/empty/with null and duplicated id inside -> correct response")
	void findByIds_various() {
		assertTrue(appUserService.findByIds(null).isEmpty());
		assertTrue(appUserService.findByIds(List.of()).isEmpty());

		var found = appUserService.findByIds(Arrays.asList(null, testAdmin.getId(), testAdmin.getId()));
		assertEquals(ONE_USER, found.size());
		assertEquals(testAdmin.getId(), found.getFirst().getId());
	}

	@Test
	@DisplayName("deleteAdminsByIds: null/empty -> empty result")
	void deleteAdminsByIds_nullOrEmpty_returnsEmpty() {
		var nullArg = appUserService.deleteAdminsByIds(null);
		var emptyListArg = appUserService.deleteAdminsByIds(List.of());
		assertTrue(nullArg.deletedIds().isEmpty() && nullArg.notFoundIds().isEmpty());
		assertTrue(emptyListArg.deletedIds().isEmpty() && emptyListArg.notFoundIds().isEmpty());
	}

	@Test
	@DisplayName("deleteAdminsByIds: not admin id present -> IllegalStateException")
	void deleteAdminsByIds_containsNonAdmin_fails() {
		assertThrows(IllegalStateException.class,
				() -> appUserService.deleteAdminsByIds(List.of(userStudent.getId(), testAdmin.getId())));
	}
}
