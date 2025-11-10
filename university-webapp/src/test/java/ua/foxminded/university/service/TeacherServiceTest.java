package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, TeacherService.class, AppUserService.class, ValidatorConfig.class,
		EntityValidatior.class, PasswordPolicy.class, PasswordEncoderConfig.class })
class TeacherServiceTest {

	static final AcademicRank RANK_A = AcademicRank.values()[0];
	static final AcademicRank RANK_B = AcademicRank.values().length > 1 ? AcademicRank.values()[1]
			: AcademicRank.values()[0];

	static final String VALID_EMAIL = "teacher1@example.com";
	static final String UPDATED_EMAIL = "teacher1updated@example.com";
	static final String DUP_EMAIL = "dup-teacher@example.com";
	static final String DUP_UPDATE_EMAIL_A = "dup-update-teacher-a@example.com";
	static final String DUP_UPDATE_EMAIL_B = "dup-update-teacher-b@example.com";

	static final String VALID_PASSWORD = "Abcd1234!";
	static final String FIRST_NAME = "Alice";
	static final String UPDATED_FIRST_NAME = "AliceUpdated";
	static final String LAST_NAME = "Smith";
	static final String UPDATED_LAST_NAME = "Smith-Updated";

	static final String OFFICE_A = "B-101";
	static final String OFFICE_B = "C-202";
	static final String BAD_OFFICE = "BAD_OFFICE";

	static final Long NON_EXISTENT_ID = 999L;

	@Autowired
	private TeacherService teacherService;

	private Teacher newTeacher(String email, String office, AcademicRank rank) {
		return Teacher.builder()
				.user(AppUser.builder()
						.email(email)
						.password(VALID_PASSWORD)
						.role(UserRole.TEACHER)
						.firstName(FIRST_NAME)
						.lastName(LAST_NAME)
						.enabled(true)
						.build())
				.office(office)
				.academicRank(rank)
				.build();
	}

	@Test
	@DisplayName("TeacherService happy path: create -> update self (email/name) -> batch update office & rank -> delete")
	void happyPath_fullCycle_success() {
		var toSave = newTeacher(VALID_EMAIL, OFFICE_A, RANK_A);

		var saved = assertDoesNotThrow(() -> teacherService.createAll(List.of(toSave)).get(0),
				"createAll(List.of(teacher)) should not throw");

		assertNotNull(saved.getId(), "Teacher ID should be assigned");
		assertNotNull(saved.getUser(), "Teacher must have AppUser");
		assertEquals(VALID_EMAIL, saved.getUser().getEmail(), "Email must be persisted");
		assertEquals(OFFICE_A, saved.getOffice(), "Office must be persisted");
		assertEquals(RANK_A, saved.getAcademicRank(), "Rank must be persisted");

		var officeUpdated = assertDoesNotThrow(() -> teacherService.updateOffices(Map.of(saved.getId(), OFFICE_B)),
				"updateOffices should not throw");
		assertEquals(1, officeUpdated, "Exactly one office must be updated");

		var rankUpdated = assertDoesNotThrow(() -> teacherService.updateAcademicRanks(Map.of(saved.getId(), RANK_B)),
				"updateAcademicRanks should not throw");
		assertEquals(1, rankUpdated, "Exactly one rank must be updated");

		var result = assertDoesNotThrow(() -> teacherService.deleteByIds(List.of(saved.getId())),
				"deleteByIds should not throw");
		assertEquals(Set.of(saved.getId()), result.deletedIds(), "Deleted must contain teacher id");
		assertTrue(result.notFoundIds().isEmpty(), "notFound must be empty");

		var afterDelete = teacherService.findByIds(List.of(saved.getId()));
		assertTrue(afterDelete.isEmpty(), "Teacher must not be found after deletion");
	}

	@Test
	@DisplayName("createAll: two teachers in one batch with same email -> DataIntegrityViolationException")
	void createAll_twoTeachersSameEmail_oneBatch_fails() {
		var t1 = newTeacher(DUP_EMAIL, OFFICE_A, RANK_A);
		var t2 = newTeacher(DUP_EMAIL, OFFICE_B, RANK_A);

		assertThrows(IllegalArgumentException.class,
				() -> teacherService.createAll(List.of(t1, t2)),
				"Batch with duplicate user.email must fail on unique constraint");
	}

	@Test
	@DisplayName("createAll: duplicate email across two calls -> DataIntegrityViolationException")
	void createAll_duplicateEmail_acrossTwoCalls_fails() {
		var t1 = newTeacher(DUP_EMAIL, OFFICE_A, RANK_A);
		assertDoesNotThrow(() -> teacherService.createAll(List.of(t1)), "First insert must pass");

		var t2 = newTeacher(DUP_EMAIL, OFFICE_B, RANK_A);

		assertThrows(IllegalArgumentException.class,
				() -> teacherService.createAll(List.of(t2)),
				"Second insert with duplicate email must fail");
	}

	@Test
	@DisplayName("createAll: user.role != TEACHER -> ConstraintViolationException")
	void createAll_invalidRole_fails() {
		var bad = Teacher.builder()
				.user(AppUser.builder()
						.email("badrole-teacher@example.com")
						.password(VALID_PASSWORD)
						.role(UserRole.STUDENT)
						.firstName(FIRST_NAME)
						.lastName(LAST_NAME)
						.enabled(true)
						.build())
				.office(OFFICE_A)
				.academicRank(RANK_A)
				.build();

		assertThrows(ConstraintViolationException.class,
				() -> teacherService.createAll(List.of(bad)),
				"Teacher.user.role must be TEACHER");
	}

	@Test
	@DisplayName("createAll: null user -> ConstraintViolationException")
	void createAll_nullUser_fails() {
		var bad = Teacher.builder().user(null).office(OFFICE_A).academicRank(RANK_A).build();

		assertThrows(ConstraintViolationException.class,
				() -> teacherService.createAll(List.of(bad)),
				"teacher.user must not be null");
	}

	@Test
	@DisplayName("createAll: null office -> ConstraintViolationException")
	void createAll_nullOffice_fails() {
		var bad = newTeacher("null-office@example.com", null, RANK_A);

		assertThrows(ConstraintViolationException.class,
				() -> teacherService.createAll(List.of(bad)),
				"teacher.office must not be null/blank and follow pattern");
	}

	@Test
	@DisplayName("createAll: null academicRank -> ConstraintViolationException")
	void createAll_nullRank_fails() {
		var bad = Teacher.builder()
				.user(AppUser.builder()
						.email("null-rank@example.com")
						.password(VALID_PASSWORD)
						.role(UserRole.TEACHER)
						.firstName(FIRST_NAME)
						.lastName(LAST_NAME)
						.enabled(true)
						.build())
				.office(OFFICE_A)
				.academicRank(null)
				.build();

		assertThrows(ConstraintViolationException.class,
				() -> teacherService.createAll(List.of(bad)),
				"teacher.academicRank must not be null");
	}

	@Test
	@DisplayName("updateOffices: empty or null map -> returns 0")
	void updateOffices_emptyOrNull_returnsZero() {
		assertEquals(0, teacherService.updateOffices(null));
		assertEquals(0, teacherService.updateOffices(Map.of()));
	}

	@Test
	@DisplayName("updateOffices: null key -> IllegalArgumentException")
	void updateOffices_nullKey_fails() {
		var saved = teacherService.createAll(List.of(newTeacher("o_nullkey@example.com", OFFICE_A, RANK_A))).get(0);

		Map<Long, String> bad = new java.util.HashMap<>();
		bad.put(null, OFFICE_B);
		bad.put(saved.getId(), OFFICE_B);

		assertThrows(IllegalArgumentException.class, () -> teacherService.updateOffices(bad), "null key must fail");
	}

	@Test
	@DisplayName("updateOffices: invalid office format -> ConstraintViolationException")
	void updateOffices_invalidOffice_fails() {
		var saved = teacherService.createAll(List.of(newTeacher("o_bad@example.com", OFFICE_A, RANK_A))).get(0);

		assertThrows(ConstraintViolationException.class,
				() -> teacherService.updateOffices(Map.of(saved.getId(), BAD_OFFICE)),
				"bad office must fail bean validation");
	}

	@Test
	@DisplayName("updateOffices: missing teacher ids -> EntityNotFoundException")
	void updateOffices_missingIds_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> teacherService.updateOffices(Map.of(NON_EXISTENT_ID, OFFICE_B)),
				"missing teacher id must fail");
	}

	@Test
	@DisplayName("updateAcademicRanks: empty or null map -> returns 0")
	void updateAcademicRanks_emptyOrNull_returnsZero() {
		assertEquals(0, teacherService.updateAcademicRanks(null));
		assertEquals(0, teacherService.updateAcademicRanks(Map.of()));
	}

	@Test
	@DisplayName("updateAcademicRanks: null key -> IllegalArgumentException")
	void updateAcademicRanks_nullKey_fails() {
		var saved = teacherService.createAll(List.of(newTeacher("r_nullkey@example.com", OFFICE_A, RANK_A))).get(0);

		Map<Long, AcademicRank> bad = new HashMap<>();
		bad.put(null, RANK_B);
		bad.put(saved.getId(), RANK_B);

		assertThrows(IllegalArgumentException.class,
				() -> teacherService.updateAcademicRanks(bad),
				"null key must fail");
	}

	@Test
	@DisplayName("updateAcademicRanks: null value -> IllegalArgumentException")
	void updateAcademicRanks_nullValue_fails() {
		var saved = teacherService.createAll(List.of(newTeacher("r_nullval@example.com", OFFICE_A, RANK_A))).get(0);

		Map<Long, AcademicRank> bad = new java.util.HashMap<>();
		bad.put(saved.getId(), null);

		assertThrows(IllegalArgumentException.class,
				() -> teacherService.updateAcademicRanks(bad),
				"null rank must fail");
	}

	@Test
	@DisplayName("updateAcademicRanks: missing teacher ids -> EntityNotFoundException")
	void updateAcademicRanks_missingIds_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> teacherService.updateAcademicRanks(Map.of(NON_EXISTENT_ID, RANK_B)),
				"missing teacher id must fail");
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixedIds_returnsSplit() {
		var saved = teacherService.createAll(List.of(newTeacher("delmix-teacher@example.com", OFFICE_A, RANK_A)))
				.get(0);

		var result = teacherService.deleteByIds(Arrays.asList(saved.getId(), NON_EXISTENT_ID, null));

		assertEquals(Set.of(saved.getId()), result.deletedIds(), "should delete existing id");
		assertEquals(Set.of(NON_EXISTENT_ID), result.notFoundIds(), "should report missing id");
	}
}
