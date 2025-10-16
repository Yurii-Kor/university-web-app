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
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, CourseService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class })
class CourseServiceTest {

	static final String CODE_ALG_LOWER = "cse-alg-101";
	static final String CODE_ALG_UPPER = "CSE-ALG-101";
	static final String CODE_SEC_LOWER = " sec-303 ";
	static final String CODE_SEC_UPPER = "SEC-303";
	static final String CODE_DB_LOWER = " db-psql-201 ";
	static final String CODE_DB_UPPER = "DB-PSQL-201";
	static final String CODE_OS_LOWER = "cse-os-302";
	static final String CODE_OS_UPPER = "CSE-OS-302";
	static final String BAD_CODE = "bad";
	static final String TO_DELETE_CODE = "CSE-100";
	static final String TO_CREATE_CODE = "SEC-420";

	static final String NAME_A = "Algorithms 101";
	static final String COURSE_SECURITY = "Security";
	static final String COURSE_SECURITY_LOWER = " security ";
	static final String COURSE_DATABASES = "Databases";
	static final String COURSE_DATABASES_LOWER = " databases ";
	static final String COURSE_OS = "Operating Systems";
	static final String COURSE_OS_LOWER = " operating systems ";

	static final String TO_DELETE_NAME = "To Delete";
	static final String TO_CREATE_NAME = "To Create";

	static final String DESC_A = "  Intro to algorithms ";
	static final String DESC_BLANK = "   ";

	static final Long NON_EXISTENT_ID = 999_999L;

	static final Integer ONE_GROUP = 1;
	static final Integer TWO_GROUPS = 2;
	static final Integer ONE_COURSE = 1;

	@Autowired
	private CourseService courseService;
	@Autowired
	private TestDataInitializer initializer;

	private Course courseSecurity, courseOperationSys;
	private Teacher testTeacher;
	private StudyGroup g1, g2;

	@BeforeAll
	void seed() {
		var testUser = AppUser.builder()
				.email("teacher@example.com")
				.password("Abcd1234!")
				.role(UserRole.TEACHER)
				.firstName("Ada")
				.lastName("Lovelace")
				.enabled(true)
				.build();

		testTeacher = initializer
				.persistAll(
						Teacher.builder().user(testUser).academicRank(AcademicRank.PROFESSOR).office("A-101").build())
				.get(0);

		g1 = initializer.persistAll(StudyGroup.builder().name("CS-201").build()).getFirst();
		g2 = initializer.persistAll(StudyGroup.builder().name("CS-202").build()).getFirst();

		courseSecurity = initializer.persistAll(newCourse(CODE_SEC_UPPER, COURSE_SECURITY, "", true, false)).get(0);

		courseOperationSys = initializer.persistAll(newCourse(CODE_OS_UPPER, COURSE_OS, "", true, true)).get(0);

	}

	private Course newCourse(String code, String name, String desc, boolean withTeacher, boolean withGroups) {
		var c = Course.builder().code(code).name(name).description(desc).build();

		if (withTeacher) {
			c.setTeacher(Teacher.builder().id(testTeacher.getId()).build());
		}
		if (withGroups) {
			c.getGroups().add(StudyGroup.builder().id(g1.getId()).build());
			c.getGroups().add(StudyGroup.builder().id(g2.getId()).build());
		}
		return c;
	}

	@Test
	@DisplayName("CourseService happy path: create -> updateSelf(name/desc) -> updateCodes -> delete")
	void happyPath_fullCycle_success() {
		var toSave = newCourse(CODE_ALG_LOWER, NAME_A, DESC_A, true, true);

		var saved = assertDoesNotThrow(() -> courseService.createAll(List.of(toSave)).getFirst(),
				"createAll(List.of(course)) should not throw");

		assertNotNull(saved.getId(), "Course id must be assigned");
		assertEquals(CODE_ALG_UPPER, saved.getCode(), "Code must be normalized to UPPER");
		assertEquals(NAME_A.trim(), saved.getName(), "Name must be trimmed");
		assertNotNull(saved.getTeacher(), "Teacher must be linked");
		assertEquals(testTeacher.getId(), saved.getTeacher().getId(), "Teacher id must match");
		assertEquals(TWO_GROUPS, saved.getGroups().size(), "Two groups must be linked");

		var patch = Course.builder().id(saved.getId()).name(COURSE_DATABASES).description(DESC_BLANK).build();

		var updated = assertDoesNotThrow(() -> courseService.updateSelf(patch), "updateSelf should not throw");

		var reloadedAfterUpdate = courseService.findByIds(List.of(updated.getId())).getFirst();
		assertEquals(COURSE_DATABASES, reloadedAfterUpdate.getName());
		assertNull(reloadedAfterUpdate.getDescription(), "Blank description must become null");

		var updatedCount = assertDoesNotThrow(
				() -> courseService.updateCodes(Map.of(reloadedAfterUpdate.getId(), CODE_DB_UPPER)),
				"updateCodes should not throw");
		assertEquals(ONE_COURSE, updatedCount);

		initializer.clear();

		var afterCode = courseService.findByIds(List.of(reloadedAfterUpdate.getId())).getFirst();
		assertEquals(CODE_DB_UPPER, afterCode.getCode(), "Code must be updated");

		var result = assertDoesNotThrow(() -> courseService.deleteByIds(List.of(afterCode.getId())),
				"deleteByIds should not throw");
		assertEquals(List.of(afterCode.getId()), result.deletedIds());
		assertTrue(result.notFoundIds().isEmpty());

		var afterDelete = courseService.findByIds(List.of(afterCode.getId()));
		assertTrue(afterDelete.isEmpty(), "Course must be gone");
	}

	@Test
	@DisplayName("createAll: null -> returns empty")
	void createAll_null_returnsEmpty() {
		assertTrue(courseService.createAll(null).isEmpty());
	}

	@Test
	@DisplayName("createAll: empty -> returns empty")
	void createAll_empty_returnsEmpty() {
		assertTrue(courseService.createAll(List.of()).isEmpty());
	}

	@Test
	@DisplayName("createAll: invalid code pattern -> ConstraintViolationException")
	void createAll_badCode_fails() {
		var bad = newCourse(BAD_CODE, NAME_A, null, false, false);
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null teacher -> ConstraintViolationException")
	void createAll_nullTeacher_fails() {
		var c = newCourse(CODE_ALG_UPPER, COURSE_SECURITY, null, false, false);
		c.setTeacher(null);
		assertThrows(ConstraintViolationException.class,
				() -> courseService.createAll(List.of(c)),
				"null teacher must fail bean validation");
	}

	@Test
	@DisplayName("createAll: teacher without id -> ConstraintViolationException")
	void createAll_teacherNullId_fails() {
		var c = newCourse(CODE_SEC_UPPER, NAME_A, null, false, false);
		c.setTeacher(Teacher.builder().id(null).build());
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: group without id -> ConstraintViolationException")
	void createAll_groupNullId_fails() {
		var c = newCourse(CODE_ALG_UPPER, COURSE_SECURITY, null, false, false);
		c.getGroups().add(StudyGroup.builder().id(null).build());
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: only nulls -> IllegalArgumentException")
	void createAll_onlyNulls_illegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(Arrays.asList(null, null)));
	}

	@Test
	@DisplayName("createAll: duplicate codes in request (case-insensitive) -> IllegalArgumentException")
	void createAll_duplicateCodesInRequest_fails() {
		var a = newCourse(CODE_DB_LOWER, NAME_A, null, true, false);
		var b = newCourse(CODE_DB_UPPER, COURSE_SECURITY, null, true, false);
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(a, b)));
	}

	@Test
	@DisplayName("createAll: duplicate names in request (case-insensitive) -> IllegalArgumentException")
	void createAll_duplicateNamesInRequest_fails() {
		var a = newCourse(CODE_SEC_UPPER, COURSE_DATABASES_LOWER, null, true, false);
		var b = newCourse(CODE_DB_UPPER, COURSE_DATABASES, null, true, false);
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(a, b)));
	}

	@Test
	@DisplayName("createAll: code already exists in DB (case-insensitive) -> IllegalArgumentException")
	void createAll_codeConflictInDb_fails() {
		var c = newCourse(CODE_SEC_LOWER, COURSE_DATABASES, null, true, false);
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: name already exists in DB (case-insensitive) -> IllegalArgumentException")
	void createAll_nameConflictInDb_fails() {
		var c = newCourse(CODE_DB_UPPER, COURSE_SECURITY_LOWER, null, true, false);
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: non-existing teacher id -> EntityNotFoundException (FK)")
	void createAll_missingTeacher_fkFails() {
		var c = newCourse(CODE_ALG_UPPER, COURSE_SECURITY, null, false, true);
		c.setTeacher(Teacher.builder().id(NON_EXISTENT_ID).build());
		assertThrows(EntityNotFoundException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: non-existing group id -> EntityNotFoundException")
	void createAll_missingGroup_notFound() {
		var c = newCourse(CODE_ALG_UPPER, COURSE_SECURITY, null, true, false);
		c.getGroups().add(StudyGroup.builder().id(NON_EXISTENT_ID).build());

		assertThrows(EntityNotFoundException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: duplicate groups in the same course -> stored as unique")
	void createAll_duplicateGroups_deduped() {
		var c = newCourse(TO_CREATE_CODE, TO_CREATE_NAME, null, true, false);
		c.getGroups().add(StudyGroup.builder().id(g1.getId()).build());
		var saved = courseService.createAll(List.of(c)).getFirst();
		assertEquals(ONE_GROUP, saved.getGroups().size());
	}

	@Test
	@DisplayName("createAll: null group element -> ConstraintViolationException")
	void createAll_groupNullElement_fails() {
		var c = newCourse(CODE_DB_UPPER, COURSE_DATABASES, null, true, false);
		c.getGroups().add(null);
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("findByIds: null/empty -> returns empty")
	void findByIds_nullOrEmpty_returnsEmpty() {
		assertTrue(courseService.findByIds(null).isEmpty());
		assertTrue(courseService.findByIds(List.of()).isEmpty());
	}

	@Test
	@DisplayName("updateSelf: null id -> IllegalArgumentException")
	void updateSelf_nullId_fails() {
		var patch = Course.builder().name(COURSE_SECURITY).build();
		assertThrows(IllegalArgumentException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: non-existing id -> EntityNotFoundException")
	void updateSelf_notFound_fails() {
		var patch = Course.builder().id(NON_EXISTENT_ID).name(COURSE_SECURITY).build();
		assertThrows(EntityNotFoundException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: no-op name (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopName_ok() {
		var patch = Course.builder().id(courseSecurity.getId()).name(COURSE_SECURITY_LOWER).build();
		var updated = courseService.updateSelf(patch);
		assertEquals(COURSE_SECURITY, updated.getName());
	}

	@Test
	@DisplayName("updateSelf: change name to existing (case-insensitive) -> IllegalArgumentException")
	void updateSelf_changeNameToExisting_fails() {
		var patch = Course.builder().id(courseSecurity.getId()).name(courseOperationSys.getName()).build();
		assertThrows(IllegalArgumentException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateCodes: duplicate codes in request -> IllegalArgumentException")
	void updateCodes_duplicateCodesInRequest_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> courseService.updateCodes(
						Map.of(courseSecurity.getId(), CODE_DB_LOWER, courseOperationSys.getId(), CODE_DB_UPPER)));
	}

	@Test
	@DisplayName("updateCodes: code conflict with DB -> IllegalArgumentException")
	void updateCodes_codeConflictInDb_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> courseService.updateCodes(Map.of(courseSecurity.getId(), CODE_OS_LOWER)));
	}

	@Test
	@DisplayName("updateCodes: no-op code -> returns 1 and unchanged")
	void updateCodes_noop_ok() {
		var count = courseService.updateCodes(Map.of(courseSecurity.getId(), CODE_SEC_LOWER));
		assertEquals(ONE_COURSE, count);
		initializer.clear();
		var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
		assertEquals(CODE_SEC_UPPER, reloaded.getCode());
	}

	@Test
	@DisplayName("updateCodes: null -> returns 0")
	void updateCodes_null_returnsZero() {
		assertEquals(0, courseService.updateCodes(null));
	}

	@Test
	@DisplayName("updateCodes: empty -> returns 0")
	void updateCodes_empty_returnsZero() {
		assertEquals(0, courseService.updateCodes(Map.of()));
	}

	@Test
	@DisplayName("updateCodes: null key -> IllegalArgumentException")
	void updateCodes_nullKey_fails() {
		var bad = new HashMap<Long, String>();
		bad.put(null, CODE_SEC_UPPER);
		bad.put(courseSecurity.getId(), CODE_SEC_UPPER);

		assertThrows(IllegalArgumentException.class, () -> courseService.updateCodes(bad));
	}

	@Test
	@DisplayName("updateCodes: null code -> IllegalArgumentException")
	void updateCodes_nullValue_fails() {
		var bad = new HashMap<Long, String>();
		bad.put(courseSecurity.getId(), null);

		assertThrows(IllegalArgumentException.class, () -> courseService.updateCodes(bad));
	}

	@Test
	@DisplayName("updateCodes: bad code pattern -> ConstraintViolationException")
	void updateCodes_badPattern_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> courseService.updateCodes(Map.of(courseSecurity.getId(), BAD_CODE)));
	}

	@Test
	@DisplayName("updateCodes: missing id with distinct code -> EntityNotFoundException")
	void updateCodes_missingId_distinctCode_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> courseService
						.updateCodes(Map.of(courseSecurity.getId(), CODE_SEC_UPPER, NON_EXISTENT_ID, CODE_DB_UPPER)));
	}

	@Test
	@DisplayName("addGroupsToCourse: happy path — adds unique groups, ignores nulls/duplicates")
	void addGroupsToCourse_happyPath_addsUnique() {
		assertDoesNotThrow(() -> courseService.addGroupsToCourse(courseSecurity.getId(),
				Arrays.asList(g1.getId(), g1.getId(), null, g2.getId())));

		initializer.clear();
		var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
		var ids = reloaded.getGroups().stream().map(StudyGroup::getId).toList();
		assertTrue(ids.containsAll(List.of(g1.getId(), g2.getId())), "course must reference both groups");
	}

	@Test
	@DisplayName("addGroupsToCourse: courseId = null -> IllegalArgumentException")
	void addGroupsToCourse_nullCourseId_fails() {
		assertThrows(IllegalArgumentException.class, () -> courseService.addGroupsToCourse(null, List.of(g1.getId())));
	}

	@Test
	@DisplayName("addGroupsToCourse: non-existing course -> EntityNotFoundException")
	void addGroupsToCourse_courseNotFound_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> courseService.addGroupsToCourse(NON_EXISTENT_ID, List.of(g1.getId())));
	}

	@Test
	@DisplayName("addGroupsToCourse: at least one group id does not exist -> EntityNotFoundException")
	void addGroupsToCourse_missingGroup_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> courseService.addGroupsToCourse(courseSecurity.getId(), List.of(NON_EXISTENT_ID)));
	}

	@Test
	@DisplayName("addGroupsToCourse: null/empty/only nulls -> returns 0 and changes nothing")
	void addGroupsToCourse_nullOrEmpty_returnsZero() {
		assertEquals(0, courseService.addGroupsToCourse(courseOperationSys.getId(), null));
		assertEquals(0, courseService.addGroupsToCourse(courseOperationSys.getId(), List.of()));
		assertEquals(0, courseService.addGroupsToCourse(courseOperationSys.getId(), Arrays.asList(null, null)));
	}

	@Test
	@DisplayName("removeGroupsFromCourse: happy path — removes only attached ids, ignores nulls/missing")
	void removeGroupsFromCourse_happyPath_removesAttached() {
		int removed = assertDoesNotThrow(() -> courseService.removeGroupsFromCourse(courseOperationSys.getId(),
				Arrays.asList(g1.getId(), NON_EXISTENT_ID, null)));

		assertEquals(ONE_GROUP, removed, "should remove exactly one attached group");
	}

	@Test
	@DisplayName("removeGroupsFromCourse: null/empty/only nulls -> returns 0")
	void removeGroupsFromCourse_nullOrEmpty_returnsZero() {
		assertEquals(0, courseService.removeGroupsFromCourse(courseOperationSys.getId(), null));
		assertEquals(0, courseService.removeGroupsFromCourse(courseOperationSys.getId(), List.of()));
		assertEquals(0, courseService.removeGroupsFromCourse(courseOperationSys.getId(), Arrays.asList(null, null)));
	}

	@Test
	@DisplayName("removeGroupsFromCourse: null courseId -> IllegalArgumentException")
	void removeGroupsFromCourse_nullCourseId_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> courseService.removeGroupsFromCourse(null, List.of(g1.getId())));
	}

	@Test
	@DisplayName("removeGroupsFromCourse: non-existing course -> EntityNotFoundException")
	void removeGroupsFromCourse_courseNotFound_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> courseService.removeGroupsFromCourse(NON_EXISTENT_ID, List.of(g1.getId())));
	}

	@Test
	@DisplayName("findByIds: contains nulls and duplicates -> unique non-null results")
	void findByIds_nullsAndDuplicates_ok() {
		var ids = Arrays.asList(courseSecurity.getId(), null, courseSecurity.getId());
		var res = courseService.findByIds(ids);
		assertEquals(ONE_COURSE, res.size());
		assertEquals(courseSecurity.getId(), res.getFirst().getId());
	}

	@Test
	@DisplayName("deleteByIds: null/empty -> returns empty result")
	void deleteByIds_nullOrEmpty_returnsEmpty() {
		var r1 = courseService.deleteByIds(null);
		var r2 = courseService.deleteByIds(List.of());
		assertTrue(r1.deletedIds().isEmpty() && r1.notFoundIds().isEmpty());
		assertTrue(r2.deletedIds().isEmpty() && r2.notFoundIds().isEmpty());
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixed_returnsSplit() {
		var c = courseService.createAll(List.of(newCourse(TO_DELETE_CODE, TO_DELETE_NAME, null, true, false))).get(0);
		var res = courseService.deleteByIds(Arrays.asList(c.getId(), NON_EXISTENT_ID, null));
		assertEquals(List.of(c.getId()), res.deletedIds(), "should delete existing id");
		assertEquals(List.of(NON_EXISTENT_ID), res.notFoundIds(), "should report missing id");
	}
}
