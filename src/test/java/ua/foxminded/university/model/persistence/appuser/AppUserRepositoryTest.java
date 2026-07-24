package ua.foxminded.university.model.persistence.appuser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import ua.foxminded.university.TestcontainersConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
		replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TestcontainersConfiguration.class)
@Sql(
		statements = {
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1001,
					'primary.admin@example.com',
					'Abcd1234!',
					'ADMIN',
					'Alice',
					'Admin',
					true
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1002,
					'second.admin@example.com',
					'Abcd1234!',
					'ADMIN',
					'Bob',
					'Builder',
					true
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1003,
					'disabled.self@example.com',
					'Abcd1234!',
					'ADMIN',
					'Carol',
					'Clark',
					false
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1004,
					'third.admin@example.com',
					'Abcd1234!',
					'ADMIN',
					'David',
					'Doe',
					true
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1005,
					'disabled.admin@example.com',
					'Abcd1234!',
					'ADMIN',
					'Eve',
					'Evans',
					false
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					2001,
					'student@example.com',
					'Abcd1234!',
					'STUDENT',
					'Grace',
					'Hopper',
					true
				);
				"""
		},
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AppUserRepositoryTest {

	static final long PRIMARY_ADMIN_ID = 1001L;
	static final long SECOND_ADMIN_ID = 1002L;
	static final long DISABLED_SELF_ADMIN_ID = 1003L;
	static final long THIRD_ADMIN_ID = 1004L;
	static final long DISABLED_ADMIN_ID = 1005L;

	static final long STUDENT_ID = 2001L;
	static final long MISSING_ID = 999_999L;

	static final int ADMIN_COUNT = 5;

	@Autowired
	private AppUserRepository appUserRepository;

	// ---------------- EXISTING EMAILS ----------------

	@Test
	@DisplayName("findExistingEmailsIgnoreCase: returns only existing emails in lowercase")
	void findExistingEmailsIgnoreCase_returnsExistingLowercaseEmails() {
		var emails = List.of(
				"primary.admin@example.com",
				"student@example.com",
				"missing@example.com");

		var existingEmails = appUserRepository
				.findExistingEmailsIgnoreCase(emails);

		assertEquals(2, existingEmails.size());

		assertEquals(
				Set.of(
						"primary.admin@example.com",
						"student@example.com"),
				Set.copyOf(existingEmails));
	}

	@Test
	@DisplayName("findExistingEmailsIgnoreCase: duplicate input values do not duplicate results")
	void findExistingEmailsIgnoreCase_duplicateInput_returnsUniqueDatabaseRows() {
		var emails = List.of(
				"primary.admin@example.com",
				"primary.admin@example.com",
				"second.admin@example.com");

		var existingEmails = appUserRepository
				.findExistingEmailsIgnoreCase(emails);

		assertEquals(2, existingEmails.size());

		assertEquals(
				Set.of(
						"primary.admin@example.com",
						"second.admin@example.com"),
				Set.copyOf(existingEmails));
	}

	@Test
	@DisplayName("findExistingEmailsIgnoreCase: no matching emails returns empty list")
	void findExistingEmailsIgnoreCase_noMatches_returnsEmptyList() {
		var existingEmails = appUserRepository
				.findExistingEmailsIgnoreCase(
						List.of(
								"missing.one@example.com",
								"missing.two@example.com"));

		assertNotNull(existingEmails);
		assertTrue(existingEmails.isEmpty());
	}

	// ---------------- ADMIN PROFILE ----------------

	@Test
	@DisplayName("findAdminProfileViewById: existing admin returns profile projection")
	void findAdminProfileViewById_existingAdmin_returnsProjection() {
		var profile = appUserRepository.findAdminProfileViewById(
				PRIMARY_ADMIN_ID);

		assertTrue(profile.isPresent());

		var adminProfile = profile.orElseThrow();

		assertEquals(
				"primary.admin@example.com",
				adminProfile.email());

		assertEquals(
				"Alice",
				adminProfile.firstName());

		assertEquals(
				"Admin",
				adminProfile.lastName());

		assertNotNull(
				adminProfile.createdAt(),
				"createdAt must be mapped to projection");
	}

	@Test
	@DisplayName("findAdminProfileViewById: disabled admin is still returned")
	void findAdminProfileViewById_disabledAdmin_returnsProjection() {
		var profile = appUserRepository.findAdminProfileViewById(
				DISABLED_ADMIN_ID);

		assertTrue(profile.isPresent());

		var adminProfile = profile.orElseThrow();

		assertEquals(
				"disabled.admin@example.com",
				adminProfile.email());

		assertEquals(
				"Eve",
				adminProfile.firstName());

		assertEquals(
				"Evans",
				adminProfile.lastName());
	}

	@Test
	@DisplayName("findAdminProfileViewById: non-admin id returns Optional.empty")
	void findAdminProfileViewById_studentId_returnsEmpty() {
		var profile = appUserRepository.findAdminProfileViewById(
				STUDENT_ID);

		assertTrue(profile.isEmpty());
	}

	@Test
	@DisplayName("findAdminProfileViewById: missing id returns Optional.empty")
	void findAdminProfileViewById_missingId_returnsEmpty() {
		var profile = appUserRepository.findAdminProfileViewById(
				MISSING_ID);

		assertTrue(profile.isEmpty());
	}

	// ---------------- ADMIN ROWS ----------------

	@Test
	@DisplayName("findAdminRowsForView: self is first even when disabled, then enabled and disabled admins")
	void findAdminRowsForView_ordersSelfEnabledAndDisabled() {
		var page = appUserRepository.findAdminRowsForView(
				DISABLED_SELF_ADMIN_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);
		assertEquals(ADMIN_COUNT, page.getTotalElements());
		assertEquals(1, page.getTotalPages());
		assertEquals(ADMIN_COUNT, page.getNumberOfElements());

		var adminIds = page.getContent()
				.stream()
				.map(admin -> admin.id())
				.toList();

		assertEquals(
				List.of(
						DISABLED_SELF_ADMIN_ID,
						PRIMARY_ADMIN_ID,
						SECOND_ADMIN_ID,
						THIRD_ADMIN_ID,
						DISABLED_ADMIN_ID),
				adminIds);
	}

	@Test
	@DisplayName("findAdminRowsForView: excludes users with non-admin role")
	void findAdminRowsForView_excludesNonAdmins() {
		var page = appUserRepository.findAdminRowsForView(
				PRIMARY_ADMIN_ID,
				PageRequest.of(0, 10));

		var adminIds = page.getContent()
				.stream()
				.map(admin -> admin.id())
				.toList();

		assertEquals(ADMIN_COUNT, adminIds.size());
		assertFalse(adminIds.contains(STUDENT_ID));
	}

	@Test
	@DisplayName("findAdminRowsForView: maps admin fields into projection")
	void findAdminRowsForView_mapsProjectionFields() {
		var page = appUserRepository.findAdminRowsForView(
				SECOND_ADMIN_ID,
				PageRequest.of(0, 10));

		var selfRow = page.getContent()
				.getFirst();

		assertEquals(SECOND_ADMIN_ID, selfRow.id());
		assertEquals(
				"second.admin@example.com",
				selfRow.email());

		assertEquals(
				"Bob",
				selfRow.firstName());

		assertEquals(
				"Builder",
				selfRow.lastName());

		assertNotNull(
				selfRow.createdAt(),
				"createdAt must be mapped to projection");

		assertTrue(selfRow.enabled());
	}

	@Test
	@DisplayName("findAdminRowsForView: count query and ordering work across pages")
	void findAdminRowsForView_returnsCorrectPaginationMetadata() {
		var firstPage = appUserRepository.findAdminRowsForView(
				DISABLED_SELF_ADMIN_ID,
				PageRequest.of(0, 3));

		assertEquals(ADMIN_COUNT, firstPage.getTotalElements());
		assertEquals(2, firstPage.getTotalPages());
		assertEquals(3, firstPage.getNumberOfElements());
		assertTrue(firstPage.hasNext());
		assertFalse(firstPage.hasPrevious());

		var firstPageIds = firstPage.getContent()
				.stream()
				.map(admin -> admin.id())
				.toList();

		assertEquals(
				List.of(
						DISABLED_SELF_ADMIN_ID,
						PRIMARY_ADMIN_ID,
						SECOND_ADMIN_ID),
				firstPageIds);

		var secondPage = appUserRepository.findAdminRowsForView(
				DISABLED_SELF_ADMIN_ID,
				PageRequest.of(1, 3));

		assertEquals(ADMIN_COUNT, secondPage.getTotalElements());
		assertEquals(2, secondPage.getTotalPages());
		assertEquals(2, secondPage.getNumberOfElements());
		assertFalse(secondPage.hasNext());
		assertTrue(secondPage.hasPrevious());

		var secondPageIds = secondPage.getContent()
				.stream()
				.map(admin -> admin.id())
				.toList();

		assertEquals(
				List.of(
						THIRD_ADMIN_ID,
						DISABLED_ADMIN_ID),
				secondPageIds);
	}
}