package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;
import ua.foxminded.university.service.dto.request.appuser.AppUserPasswordChangeDto;
import ua.foxminded.university.service.dto.request.appuser.AppUserSelfUpdateDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, AppUserService.class, ValidatorConfig.class, EntityValidatior.class,
		PasswordPolicy.class, PasswordEncoderConfig.class, TestDataInitializer.class, DtoMapper.class,
		DuplicateGuard.class })
class AppUserServiceTest {

	private static final String DEFAULT_EMAIL = "admin@example.com";
	private static final String UPDATED_EMAIL = "admin.updated@example.com";

	private static final String TEST_ADMIN_EMAIL = "admin.test@example.com";
	private static final String TAKEN_EMAIL = "student.taken@example.com";

	private static final String FIRST_NAME = "Alice";
	private static final String LAST_NAME = "Cooper";

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

	private AppUser testAdmin, userStudent, tempAdmin;

	private AppUser draftAdminEntity(String email) {
		return AppUser.builder()
				.email(email)
				.password(passwordPolicy.encodePassword(PWD))
				.role(UserRole.ADMIN)
				.firstName(FIRST_NAME)
				.lastName(LAST_NAME)
				.enabled(true)
				.build();
	}

	private AppUserCreateDto newAdminDto(String email) {
		return new AppUserCreateDto(email, PWD, "Alice", "Admin");
	}

	private AppUserSelfUpdateDto patchProfileDto(Long id, String email, String first, String last) {
		return new AppUserSelfUpdateDto(id, email, first, last);
	}

	private AppUserPasswordChangeDto changePasswordDto(Long id, String current, String next) {
		return new AppUserPasswordChangeDto(id, current, next);
	}

	@BeforeAll
	void setup() {
		testAdmin = initializer.persistAll(draftAdminEntity(TEST_ADMIN_EMAIL)).getFirst();

		userStudent = initializer.persistAll(AppUser.builder()
				.email(TAKEN_EMAIL)
				.password(PWD)
				.role(UserRole.STUDENT)
				.firstName("Bob")
				.lastName("User")
				.enabled(true)
				.build()).getFirst();
	}

	@AfterEach
	void cleanup() {
		Optional.ofNullable(tempAdmin).ifPresent(user -> appUserService.deleteAdminsByIds(List.of(tempAdmin.getId())));
	}

	@Test
	@DisplayName("createAdmins: happy path — creates enabled ADMIN with encoded password")
	void createAdmins_happyPath_success() {
		var createdAll = appUserService.createAdmins(List.of(newAdminDto(DEFAULT_EMAIL)));

		assertEquals(ONE_USER, createdAll.size());
		tempAdmin = createdAll.getFirst();

		assertNotNull(tempAdmin.getId());
		assertEquals(DEFAULT_EMAIL, tempAdmin.getEmail());
		assertEquals(UserRole.ADMIN, tempAdmin.getRole());
		assertTrue(tempAdmin.isEnabled());

		passwordPolicy.assertCurrentMatches(PWD, tempAdmin.getPassword());
	}

	@Test
	@DisplayName("deleteAdminsByIds: guard — can't delete last admin")
	void deleteAdminsByIds_lastAdmin_guard_fails() {
		assertThrows(IllegalStateException.class, () -> appUserService.deleteAdminsByIds(List.of(testAdmin.getId())));
	}

	@Test
	@DisplayName("createAdmins: null/empty -> empty list")
	void createAdmins_nullOrEmpty_returnsEmpty() {
		assertTrue(appUserService.createAdmins(null).isEmpty());
		assertTrue(appUserService.createAdmins(List.of()).isEmpty());
	}

	@Test
	@DisplayName("createAdmins: null email -> ConstraintViolationException")
	void createAdmins_nullEmail_fails() {
		var bad = new AppUserCreateDto(null, PWD, "Alice", "Admin");
		assertThrows(ConstraintViolationException.class, () -> appUserService.createAdmins(List.of(bad)));
	}

	@Test
	@DisplayName("createAdmins: null password -> ConstraintViolationException")
	void createAdmins_nullPassword_fails() {
		var bad = new AppUserCreateDto(DEFAULT_EMAIL, null, "Alice", "Admin");
		assertThrows(ConstraintViolationException.class, () -> appUserService.createAdmins(List.of(bad)));
	}

	@Test
	@DisplayName("createAdmins: duplicate emails in one batch (case-insensitive) -> IllegalArgumentException")
	void createAdmins_duplicateEmailsInOneBatch_fails() {
		var dupLower = newAdminDto(DEFAULT_EMAIL);
		var dupUpper = newAdminDto(DEFAULT_EMAIL.toUpperCase());
		assertThrows(IllegalArgumentException.class, () -> appUserService.createAdmins(List.of(dupLower, dupUpper)));
	}

	@Test
	@DisplayName("createAdmins: duplicate email of created user -> IllegalArgumentException")
	void createAdmins_duplicateAcrossCalls_fails() {
		var dup = newAdminDto(testAdmin.getEmail());
		assertThrows(IllegalArgumentException.class, () -> appUserService.createAdmins(List.of(dup)));
	}
	
	@Test
	@DisplayName("listAdmins: returns only admins and contains existing ADMIN")
	void listAdmins_onlyAdmins_containsTestAdmin() {
	    var rows = appUserService.listAdmins();

	    assertNotNull(rows);
	    assertFalse(rows.isEmpty(), "Must contain at least one admin (seeded in @BeforeAll)");

	    var me = rows.stream()
	            .filter(r -> Objects.equals(r.id(), testAdmin.getId()))
	            .findFirst()
	            .orElseThrow(() -> new AssertionError("Expected testAdmin to be present in listAdmins()"));

	    assertEquals(testAdmin.getEmail(), me.email());
	    assertEquals(testAdmin.getFirstName(), me.firstName());
	    assertEquals(testAdmin.getLastName(), me.lastName());
	    assertEquals(testAdmin.isEnabled(), me.enabled());
	    assertNotNull(me.createdAt(), "createdAt must be populated by DB");

	    assertTrue(rows.stream().noneMatch(r -> Objects.equals(r.id(), userStudent.getId())),
	            "Non-admin users must not appear in admin rows");
	}

	@Test
	@DisplayName("updateProfileFields: happy path — email + first/last name updated")
	void updateProfileFields_happyPath_success() {
		tempAdmin = appUserService.createAdmins(List.of(newAdminDto(DEFAULT_EMAIL))).getFirst();
		appUserService.updateProfileFields(
				patchProfileDto(tempAdmin.getId(), UPDATED_EMAIL, UPDATED_FIRST_NAME, UPDATED_LAST_NAME));

		var reloaded = appUserService.findByIds(List.of(tempAdmin.getId())).getFirst();

		assertEquals(UPDATED_EMAIL, reloaded.getEmail());
		assertEquals(UPDATED_FIRST_NAME, reloaded.getFirstName());
		assertEquals(UPDATED_LAST_NAME, reloaded.getLastName());
	}

	@Test
	@DisplayName("updateProfileFields: no-op — same email (trim/case-insensitive) and null names")
	void updateProfileFields_noop_sameEmail_ok() {
		var patch = patchProfileDto(testAdmin.getId(), testAdmin.getEmail(), null, null);
		appUserService.updateProfileFields(patch);

		var found = appUserService.findByIds(List.of(testAdmin.getId())).getFirst();

		assertEquals(testAdmin.getEmail(), found.getEmail(), "Email must remain unchanged");
		assertEquals(testAdmin.getFirstName(), found.getFirstName(), "First name must remain unchanged");
		assertEquals(testAdmin.getLastName(), found.getLastName(), "Last name must remain unchanged");
	}

	@Test
	@DisplayName("updateProfileFields: dto is null -> IllegalArgumentException")
	void updateProfileFields_nullArgs_fails() {
		assertThrows(IllegalArgumentException.class, () -> appUserService.updateProfileFields(null));
	}

	@Test
	@DisplayName("updateProfileFields: user not found -> EntityNotFoundException")
	void updateProfileFields_notFound_fails() {
		var patchMissing = patchProfileDto(MISSING_ID, UPDATED_EMAIL, null, null);
		assertThrows(EntityNotFoundException.class, () -> appUserService.updateProfileFields(patchMissing));
	}

	@Test
	@DisplayName("updateProfileFields: set email that belongs to another user -> IllegalArgumentException")
	void updateProfileFields_emailTaken_fails() {
		var patchWithTakenEmail = patchProfileDto(testAdmin.getId(), TAKEN_EMAIL, null, null);
		assertThrows(IllegalArgumentException.class, () -> appUserService.updateProfileFields(patchWithTakenEmail));
	}

	@Test
	@DisplayName("enableUsersByIds: contains missing id -> EntityNotFoundException")
	void enableUsersByIds_containsMissingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> appUserService.enableUsersByIds(List.of(MISSING_ID)));
	}

	@Test
	@DisplayName("disableUsersByIds: can disable non-admin user")
	void disableUsersByIds_student_ok() {
		appUserService.disableUsersByIds(List.of(userStudent.getId()));

		var reloaded = appUserService.findByIds(List.of(userStudent.getId())).getFirst();
		assertFalse(reloaded.isEnabled(), "Student must become disabled");
	}

	@Test
	@DisplayName("disableUsersByIds: can enable non-admin user")
	void enableUsersByIds_student_ok() {
		appUserService.enableUsersByIds(List.of(userStudent.getId()));

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
	@DisplayName("changePasswordSelf: happy path — пароль changed and encoded")
	void changePasswordSelf_happyPath_success() {
		appUserService.changePasswordSelf(changePasswordDto(testAdmin.getId(), PWD, PWD_NEW));

		var updated = appUserService.findByIds(List.of(testAdmin.getId())).getFirst();

		assertNotEquals(passwordPolicy.encodePassword(PWD), updated.getPassword());
		passwordPolicy.assertCurrentMatches(PWD_NEW, updated.getPassword());
	}

	@Test
	@DisplayName("changePasswordSelf: missing required fields -> ConstraintViolationException")
	void changePasswordSelf_missingFields_fails() {
		// id=null
		assertThrows(ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(new AppUserPasswordChangeDto(null, PWD, PWD_NEW)));

		// currentPassword=null
		assertThrows(ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(changePasswordDto(testAdmin.getId(), null, PWD_NEW)));

		// newPassword=null
		assertThrows(ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(changePasswordDto(testAdmin.getId(), PWD, null)));
	}

	@Test
	@DisplayName("changePasswordSelf: user not found -> EntityNotFoundException")
	void changePasswordSelf_notFound_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> appUserService.changePasswordSelf(changePasswordDto(MISSING_ID, PWD, PWD_NEW)));
	}

	@Test
	@DisplayName("changePasswordSelf: wrong current password -> IllegalArgumentException")
	void changePasswordSelf_wrongCurrent_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> appUserService.changePasswordSelf(changePasswordDto(testAdmin.getId(), PWD_NEW, PWD_NEW)));
	}

	@Test
	@DisplayName("changePasswordSelf: new password violates pattern -> ConstraintViolationException")
	void changePasswordSelf_badNewPassword_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> appUserService.changePasswordSelf(changePasswordDto(testAdmin.getId(), PWD, PWD_BAD)));
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
	@DisplayName("deleteAdminsByIds: happy path — delete + notFound")
	void deleteAdminsByIds_happyPath_success() {
		var createdAll = appUserService.createAdmins(List.of(newAdminDto(DEFAULT_EMAIL)));
		var created = createdAll.getFirst();

		var del = appUserService.deleteAdminsByIds(List.of(created.getId(), MISSING_ID));

		assertEquals(Set.of(created.getId()), del.deletedIds());
		assertEquals(Set.of(MISSING_ID), del.notFoundIds());
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

	@Test
	@DisplayName("getAdminProfileView: happy path — returns AdminProfileView for existing ADMIN")
	void getAdminProfileView_happyPath_success() {
		var view = appUserService.getAdminProfileView(testAdmin.getId());

		assertNotNull(view);
		assertEquals(testAdmin.getEmail(), view.email());
		assertEquals(testAdmin.getFirstName(), view.firstName());
		assertEquals(testAdmin.getLastName(), view.lastName());
		assertNotNull(view.createdAt(), "createdAt must be populated by DB");
	}

	@Test
	@DisplayName("getAdminProfileView: missing id -> EntityNotFoundException")
	void getAdminProfileView_missingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> appUserService.getAdminProfileView(MISSING_ID));
	}
}
