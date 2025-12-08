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
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.dto.request.studygroup.StudyGroupCreateDto;
import ua.foxminded.university.service.dto.request.studygroup.StudyGroupRenameDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, StudyGroupService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class, DtoMapper.class, DuplicateGuard.class })
class StudyGroupServiceTest {

	static final String DEFAULT_GROUP_NAME = "CS-101";
	static final String NAME_A = "AB-101";
	static final String NAME_B = "SE-205";
	static final String TO_CREATE_VALID = "EE-205";
	static final String TO_DELETE = "CS-102";

	static final String BAD_NAME = "wrong";
	static final String BAD_SPACED_LOWER = "  e  e  205 ";

	static final Long MISSING_ID = 999_999L;
	static final Integer ONE_GROUP = 1;

	@Autowired
	private StudyGroupService groupService;
	@Autowired
	private TestDataInitializer initializer;

	private StudyGroup testGroup, groupWithStudent, tempGroup;

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
	
	@AfterEach
	void cleanup() {
		Optional.ofNullable(tempGroup).ifPresent(user -> groupService.deleteByIds(List.of(tempGroup.getId())));
		tempGroup = null;
	}

	@Test
	@DisplayName("createAll: happy path — ignores nulls and persists valid group")
	void createAll_happyPath_success() {
		var dto = new StudyGroupCreateDto(NAME_A);

		var savedAll = groupService.createAll(Arrays.asList(null, dto));

		assertEquals(ONE_GROUP, savedAll.size(), "null DTO must be ignored");
		tempGroup = savedAll.getFirst();

		assertNotNull(tempGroup.getId(), "Group id must be assigned");
		assertEquals(NAME_A, tempGroup.getName(), "Name must be persisted as provided");
	}

	@Test
	@DisplayName("createAll: invalid pattern (lowercase + spaces) -> ConstraintViolationException")
	void createAll_invalidPattern_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> groupService.createAll(List.of(new StudyGroupCreateDto(BAD_SPACED_LOWER))),
				"Bad group name must fail bean validation");
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
	@DisplayName("createAll: only nulls -> returns empty list")
	void createAll_onlyNulls_returnsEmptyList() {
		var result = groupService.createAll(Arrays.asList(null, null));
		assertNotNull(result, "result must not be null");
		assertTrue(result.isEmpty(), "only-null input must result in empty list");
	}

	@Test
	@DisplayName("createAll: invalid name -> ConstraintViolationException")
	void createAll_invalidName_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> groupService.createAll(List.of(new StudyGroupCreateDto(BAD_NAME))),
				"Bad group name must fail bean validation");
	}

	@Test
	@DisplayName("createAll: duplicate name in DB -> IllegalArgumentException")
	void createAll_duplicateName_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.createAll(List.of(new StudyGroupCreateDto(DEFAULT_GROUP_NAME))),
				"duplicate group name should fail");
	}

	@Test
	@DisplayName("createAll: duplicate names in one request -> IllegalArgumentException")
	void createAll_duplicatesInRequest_fail() {
		var a = new StudyGroupCreateDto(NAME_B);
		var b = new StudyGroupCreateDto(NAME_B);
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
	@DisplayName("rename: happy path — updates group name")
	void rename_happyPath_success() {
		tempGroup = groupService.createAll(List.of(new StudyGroupCreateDto(NAME_A))).getFirst();

		var renamed = groupService.rename(new StudyGroupRenameDto(tempGroup.getId(), NAME_B));

		assertEquals(tempGroup.getId(), renamed.getId());
		assertEquals(NAME_B, renamed.getName(), "Name must be updated");
	}

	@Test
	@DisplayName("rename: null patch -> IllegalArgumentException")
	void rename_nullPatch_fails() {
		assertThrows(IllegalArgumentException.class, () -> groupService.rename(null));
	}

	@Test
	@DisplayName("rename: null id -> ConstraintViolationException")
	void rename_nullId_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> groupService.rename(new StudyGroupRenameDto(null, NAME_B)));
	}

	@Test
	@DisplayName("rename: bad pattern -> ConstraintViolationException")
	void rename_badPattern_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> groupService.rename(new StudyGroupRenameDto(testGroup.getId(), BAD_NAME)),
				"bad pattern must fail bean validation");
	}

	@Test
	@DisplayName("rename: missing id with valid name -> EntityNotFoundException")
	void rename_missingId_distinctName_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> groupService.rename(new StudyGroupRenameDto(MISSING_ID, NAME_B)));
	}

	@Test
	@DisplayName("rename: target name already exists in DB -> IllegalArgumentException")
	void rename_conflictInDb_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> groupService.rename(new StudyGroupRenameDto(testGroup.getId(), groupWithStudent.getName())),
				"renaming to existing DB name must fail");
	}

	@Test
	@DisplayName("rename: no-op — new name equals current one -> returns entity unchanged")
	void rename_noop_sameName_success() {
		var renamed = groupService.rename(new StudyGroupRenameDto(testGroup.getId(), DEFAULT_GROUP_NAME));
		assertEquals(testGroup.getId(), renamed.getId());
		assertEquals(DEFAULT_GROUP_NAME, renamed.getName());
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixed_returnsSplit() {
		var g = groupService.createAll(List.of(new StudyGroupCreateDto(TO_DELETE))).get(0);

		var res = groupService.deleteByIds(Arrays.asList(g.getId(), MISSING_ID, null));
		assertEquals(Set.of(g.getId()), res.deletedIds(), "should delete existing id");
		assertEquals(Set.of(MISSING_ID), res.notFoundIds(), "should report missing id");
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
		assertEquals(Set.of(MISSING_ID), res.notFoundIds(), "missing should be reported");
	}

	@Test
	@DisplayName("deleteByIds: duplicated ids in input -> deletedIds unique")
	void deleteByIds_duplicatedInput_ok() {
		var g = groupService.createAll(List.of(new StudyGroupCreateDto(TO_DELETE))).get(0);
		var res = groupService.deleteByIds(Arrays.asList(g.getId(), g.getId()));
		assertEquals(Set.of(g.getId()), res.deletedIds());
		assertTrue(res.notFoundIds().isEmpty());
	}
}
