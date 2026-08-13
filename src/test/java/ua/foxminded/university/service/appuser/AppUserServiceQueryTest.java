package ua.foxminded.university.service.appuser;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.appuser.AppUserServiceTestConstants.*;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

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
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;

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
class AppUserServiceQueryTest {

	@Autowired
	private AppUserService appUserService;

	@Test
	@DisplayName("listAdmins: returns paged admin rows only")
	void listAdmins_returnsAdminsOnly() {
		var dto = new AppUserCreateDto(
				ADMIN_LIST_EMAIL,
				PWD,
				"Alice",
				"Admin");

		var createdAdmin = appUserService.createAdmin(dto);

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
		assertEquals(
				TEST_ADMIN_ID,
				found.getFirst().getId());
	}

	@Test
	@DisplayName("getAdminProfileView: happy path — returns AdminProfileView for existing ADMIN")
	void getAdminProfileView_happyPath_success() {
		var view = appUserService.getAdminProfileView(
				TEST_ADMIN_ID);

		assertNotNull(view);
		assertEquals(TEST_ADMIN_EMAIL, view.email());
		assertEquals(FIRST_NAME, view.firstName());
		assertEquals(LAST_NAME, view.lastName());

		assertNotNull(
				view.createdAt(),
				"createdAt must be populated by DB");
	}

	@Test
	@DisplayName("getAdminProfileView: student id -> EntityNotFoundException")
	void getAdminProfileView_studentId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.getAdminProfileView(
						STUDENT_USER_ID));
	}

	@Test
	@DisplayName("getAdminProfileView: missing id -> EntityNotFoundException")
	void getAdminProfileView_missingId_fails() {
		assertThrows(
				EntityNotFoundException.class,
				() -> appUserService.getAdminProfileView(
						MISSING_ID));
	}
}