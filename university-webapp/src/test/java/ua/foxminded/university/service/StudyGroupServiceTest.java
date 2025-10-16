package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, StudyGroupService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class })
class StudyGroupServiceTest {

	static final String DEFAULT_GROUP_NAME = "CS-101";
	static final String GROUP_WITH_STUDENTS = "CS-100";
	static final String TO_CREATE_LOWER_SPACED = "  e  e  205 ";
	static final String TO_CREATE_NORMALIZED = "EE-205";
	static final String TO_DELETE = "CS-102";
	static final String NAME_A_LOWER_SPACED = "  ab-101 ";
	static final String NAME_A_NORMALIZED = "AB-101";
	static final String NAME_B_LOWER = "se-205";
	static final String NAME_B_NORMALIZED = "SE-205";

	static final String BAD_NAME = "wrong";
	static final Long MISSING_ID = 999_999L;

	static final Integer ONE_GROUP = 1;

	@Autowired
	private StudyGroupService groupService;

	@Autowired
	private TestDataInitializer initializer;

	private StudyGroup testGroup;
	private StudyGroup groupWithStudent;

	@BeforeAll
	void setup() {
		testGroup = initializer.persistAll(StudyGroup.builder().name(DEFAULT_GROUP_NAME).build()).get(0);

		var group = StudyGroup.builder().name("CS-201").build();
		var student = Student.builder()
				.user(AppUser.builder()
						.email("seed-student@example.com")
						.password("Abcd1234!")
						.role(UserRole.STUDENT)
						.firstName("Seed")
						.lastName("Student")
						.enabled(true)
						.build())
				.group(group)
				.enrollmentYear(2024)
				.build();

		groupWithStudent = initializer.persistAll(group).get(0);
		initializer.persistAll(student).get(0);
	}

	@Test
	@DisplayName("StudyGroupService happy path: create -> rename -> delete")
	void happyPath_fullCycle_success() {
		var toSave = StudyGroup.builder().name(NAME_A_LOWER_SPACED).build();
		var savedAll = assertDoesNotThrow(() -> groupService.createAll(Arrays.asList(null, toSave)),
				"createAll(List.of(group)) should not throw");

		assertEquals(ONE_GROUP, savedAll.size(), "Null should be ignored");

		var saved = savedAll.getFirst();

		assertNotNull(saved.getId(), "Group id must be assigned");
		assertEquals(NAME_A_NORMALIZED, saved.getName(), "Name must be normalized & persisted");

		var updatedCount = assertDoesNotThrow(() -> groupService.rename(Map.of(saved.getId(), NAME_B_LOWER)),
				"rename should not throw");
		assertEquals(1, updatedCount, "Exactly one group must be updated");

		var afterUpdate = groupService.findByIds(List.of(saved.getId()));
		assertFalse(afterUpdate.isEmpty(), "Group must be found after rename");
		assertEquals(NAME_B_NORMALIZED, afterUpdate.get(0).getName(), "Name must be updated & normalized");

		var result = assertDoesNotThrow(() -> groupService.deleteByIds(List.of(saved.getId())),
				"deleteByIds should not throw");
		assertEquals(List.of(saved.getId()), result.deletedIds(), "Deleted must contain id");
		assertTrue(result.notFoundIds().isEmpty(), "notFound must be empty");

		var afterDelete = groupService.findByIds(List.of(saved.getId()));
		assertTrue(afterDelete.isEmpty(), "Group must not be found after deletion");
	}

	@Test
	@DisplayName("createAll: whitespace-insensitive normalization")
	void createAll_weirdWhitespaces_ok() {
		var saved = groupService.createAll(List.of(StudyGroup.builder().name(TO_CREATE_LOWER_SPACED).build()));
		assertEquals(TO_CREATE_NORMALIZED, saved.getFirst().getName());
	}

	@Test
	@DisplayName("createAll: null -> returns empty")
	void createAll_null_returnsEmpty() {
		assertTrue(groupService.createAll(null).isEmpty(), "null list must return empty result");
	}

	@Test
	@DisplayName("createAll: empty -> returns empty")
	void createAll_empty_returnsEmpty() {
		assertTrue(groupService.createAll(List.of()).isEmpty(), "empty list must return empty result");
	}

	@Test
	@DisplayName("createAll: only nulls -> IllegalArgumentException (после фильтра список пуст)")
	void createAll_onlyNulls_illegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.createAll(Arrays.asList(null, null)),
				"only-null input must fail with IllegalArgumentException");
	}

	@Test
	@DisplayName("createAll: invalid name -> ConstraintViolationException")
	void createAll_invalidName_fails() {
		var bad = StudyGroup.builder().name(BAD_NAME).build();
		assertThrows(ConstraintViolationException.class,
				() -> groupService.createAll(List.of(bad)),
				"Bad group name must fail bean validation");
	}

	@Test
	@DisplayName("createAll: duplicate name in DB -> fails")
	void createAll_duplicateName_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.createAll(List.of(StudyGroup.builder().name(DEFAULT_GROUP_NAME).build())),
				"duplicate group name should fail");
	}

	@Test
	@DisplayName("createAll: duplicate normalized names in one request -> IllegalArgumentException")
	void createAll_duplicatesInRequest_fail() {
		var a = StudyGroup.builder().name(NAME_B_LOWER).build();
		var b = StudyGroup.builder().name(NAME_B_NORMALIZED).build();
		assertThrows(IllegalArgumentException.class, () -> groupService.createAll(List.of(a, b)));
	}

	@Test
	@DisplayName("findByIds: null/empty -> returns empty")
	void findByIds_nullOrEmpty_returnsEmpty() {
		assertTrue(groupService.findByIds(null).isEmpty(), "null ids must return empty");
		assertTrue(groupService.findByIds(List.of()).isEmpty(), "empty ids must return empty");
	}

	@Test
	@DisplayName("findByIds: list with null inside - null is ignored, duplicate ids are removed")
	void findByIds_ignoresNullsAndDedups() {
		var res = groupService.findByIds(Arrays.asList(null, testGroup.getId(), null, testGroup.getId()));
		assertEquals(ONE_GROUP, res.size(), "one result should be returned");
		assertEquals(testGroup.getId(), res.getFirst().getId());
	}

	@Test
	@DisplayName("rename: null map -> returns 0")
	void rename_nullMap_returnsZero() {
		assertEquals(0, groupService.rename(null), "null map must return 0");
	}

	@Test
	@DisplayName("rename: empty map -> returns 0")
	void rename_emptyMap_returnsZero() {
		assertEquals(0, groupService.rename(Map.of()), "empty map must return 0");
	}

	@Test
	@DisplayName("rename: null key -> IllegalArgumentException")
	void rename_nullKey_fails() {
		var bad = new HashMap<Long, String>();
		bad.put(null, NAME_B_LOWER);
		bad.put(testGroup.getId(), NAME_B_LOWER);

		assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(bad),
				"null key must fail with IllegalArgumentException");
	}

	@Test
	@DisplayName("rename: duplicates in request are validated before missing id -> IllegalArgumentException")
	void rename_duplicatesOvershadowMissingId_fails() {
		var ex = assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(Map.of(testGroup.getId(), NAME_B_LOWER, MISSING_ID, NAME_B_LOWER)),
				"duplicate names in request should fail before checking missing ids");
		assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), "Should fail specifically on duplicate names");
	}

	@Test
	@DisplayName("rename: missing id with distinct, valid names -> EntityNotFoundException")
	void rename_missingId_distinctNames_fails() {
		assertThrows(EntityNotFoundException.class, () -> groupService.rename(Map.of(MISSING_ID, NAME_B_NORMALIZED)));
	}

	@Test
	@DisplayName("rename: null value -> IllegalArgumentException")
	void rename_nullValue_fails() {
		var bad = new HashMap<Long, String>();
		bad.put(testGroup.getId(), null);

		assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(bad),
				"null name must fail bean validation");
	}

	@Test
	@DisplayName("rename: bad pattern -> ConstraintViolationException")
	void rename_badPattern_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> groupService.rename(Map.of(testGroup.getId(), BAD_NAME)),
				"bad pattern must fail bean validation");
	}

	@Test
	@DisplayName("rename: duplicate normalized names in request -> IllegalArgumentException")
	void rename_duplicateNamesInRequest_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(
						Map.of(testGroup.getId(), NAME_A_LOWER_SPACED, groupWithStudent.getId(), NAME_A_NORMALIZED)),
				"duplicate names in request must fail");
	}

	@Test
	@DisplayName("rename: target name already exists in DB -> IllegalArgumentException")
	void rename_conflictInDb_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(Map.of(testGroup.getId(), groupWithStudent.getName())),
				"renaming to existing DB name must fail");
	}

	@Test
	@DisplayName("rename: no-op — the new name matches the current one (after normalization) -> gets 1 and the name does not change")
	void rename_noop_sameName_success() {
		var updated = groupService.rename(Map.of(testGroup.getId(), DEFAULT_GROUP_NAME));
		assertEquals(ONE_GROUP, updated, "Ірщгдв иу ");
	}

	@Test
	@DisplayName("rename: swap A↔B in one batch — expected an Exception in the unique index")
	void rename_swapAB_batch_failsOnUniqueIndex() {
		assertThrows(DataIntegrityViolationException.class,
				() -> groupService.rename(Map.of(testGroup.getId(),
						groupWithStudent.getName(),
						groupWithStudent.getId(),
						testGroup.getName())),
				"swap in one batch usually breaks due to the unique index");
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixed_returnsSplit() {
		var g = groupService.createAll(List.of(StudyGroup.builder().name(TO_DELETE).build())).get(0);

		var res = groupService.deleteByIds(Arrays.asList(g.getId(), MISSING_ID, null));
		assertEquals(List.of(g.getId()), res.deletedIds(), "should delete existing id");
		assertEquals(List.of(MISSING_ID), res.notFoundIds(), "should report missing id");
	}

	@Test
	@DisplayName("deleteByIds: cannot delete groups that still have students")
	void deleteByIds_groupsWithStudents_fails() {
		assertThrows(IllegalStateException.class,
				() -> groupService.deleteByIds(List.of(groupWithStudent.getId())),
				"Should fail when group is not empty");
	}

	@Test
	@DisplayName("deleteByIds: only missing/null -> empty deleted, notFound has ids")
	void deleteByIds_onlyMissing_returnsNotFound() {
		var res = groupService.deleteByIds(Arrays.asList(null, MISSING_ID));
		assertTrue(res.deletedIds().isEmpty(), "nothing should be deleted");
		assertEquals(List.of(MISSING_ID), res.notFoundIds(), "missing should be reported");
	}

	@Test
	@DisplayName("deleteByIds: duplicated ids in input -> deletedIds unique")
	void deleteByIds_duplicatedInput_ok() {
		var g = groupService.createAll(List.of(StudyGroup.builder().name(TO_DELETE).build())).get(0);
		var res = groupService.deleteByIds(Arrays.asList(g.getId(), g.getId()));
		assertEquals(List.of(g.getId()), res.deletedIds());
		assertTrue(res.notFoundIds().isEmpty());
	}
}
