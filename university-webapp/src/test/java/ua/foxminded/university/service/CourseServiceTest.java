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
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseUpdateCodesDto;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, CourseService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class, DtoMapper.class, DuplicateGuard.class })
class CourseServiceTest {

	static final String CODE_ALG_UPPER = "CSE-ALG-101";
	static final String CODE_SEC_UPPER = "SEC-303";
	static final String CODE_DB_UPPER = "DB-PSQL-201";
	static final String CODE_OS_UPPER = "CSE-OS-302";
	static final String BAD_CODE = "bad";
	static final String TO_DELETE_CODE = "CSE-100";
	static final String TO_CREATE_CODE = "SEC-420";

	static final String COURSE_ALG = "Algorithms 101";
	static final String COURSE_SECURITY = "Security";
	static final String COURSE_SECURITY_LOWER = "security";
	static final String COURSE_DATABASES = "Databases";
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

	private Course courseSecurity, courseOperationSys, tempCourse;
	private Teacher testTeacher;
	private StudyGroup g1, g2;

	private CourseCreateDto newCreateDto(String code, String name, String desc, Long teacherId) {
		return new CourseCreateDto(code, name, desc, teacherId);
	}

	private CourseSelfUpdateDto newUpdateSelf(Long id, String name, String desc) {
		return new CourseSelfUpdateDto(id, name, desc);
	}

	private CourseUpdateCodesDto newUpdateCode(Long id, String code) {
		return new CourseUpdateCodesDto(id, code);
	}

	@BeforeAll
	void setup() {
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

		courseOperationSys = initializer.persistAll(Course.builder()
				.code(CODE_OS_UPPER)
				.name(COURSE_OS)
				.description("")
				.teacher(Teacher.builder().id(testTeacher.getId()).build())
				.groups(new LinkedHashSet<>(Arrays.asList(StudyGroup.builder().id(g1.getId()).build(),
						StudyGroup.builder().id(g2.getId()).build())))
				.build()).get(0);
	}

	@BeforeEach
	void setupCourseSecurity() {
		courseSecurity = courseService
				.createAll(List.of(newCreateDto(CODE_SEC_UPPER, COURSE_SECURITY, "", testTeacher.getId())))
				.getFirst();
	}

	@AfterEach
	void cleanup() {
		var ids = new ArrayList<Long>();

		Optional.ofNullable(tempCourse).map(Course::getId).ifPresent(ids::add);
		Optional.ofNullable(courseSecurity).map(Course::getId).ifPresent(ids::add);

		if (!ids.isEmpty()) {
			courseService.deleteByIds(ids);
		}
	}

	@Test
	@DisplayName("createAll: happy path — creates course with teacher and normalized name")
	void createAll_happyPath_success() {
		var dto = newCreateDto(CODE_ALG_UPPER, COURSE_ALG, DESC_A, testTeacher.getId());

		tempCourse = courseService.createAll(List.of(dto)).getFirst();

		assertNotNull(tempCourse.getId());
		assertEquals(CODE_ALG_UPPER, tempCourse.getCode());
		assertEquals(COURSE_ALG.trim(), tempCourse.getName());
		assertEquals(testTeacher.getId(), tempCourse.getTeacher().getId());
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
	@DisplayName("createAll: invalid code pattern (DTO) -> ConstraintViolationException")
	void createAll_badCode_fails() {
		var bad = newCreateDto(BAD_CODE, COURSE_ALG, null, testTeacher.getId());
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null teacherId in DTO -> ConstraintViolationException")
	void createAll_nullTeacher_fails() {
		var c = newCreateDto(CODE_ALG_UPPER, COURSE_SECURITY, null, null);
		assertThrows(ConstraintViolationException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: only nulls -> returns empty list")
	void createAll_onlyNulls_returnsEmptyList() {
		var result = courseService.createAll(Arrays.asList(null, null));
		assertNotNull(result, "result must not be null");
		assertTrue(result.isEmpty(), "only-null input must result in empty list");
	}

	@Test
	@DisplayName("createAll: duplicate codes in request (case-insensitive) -> IllegalArgumentException")
	void createAll_duplicateCodesInRequest_fails() {
		var a = newCreateDto(CODE_DB_UPPER, COURSE_ALG, null, testTeacher.getId());
		var b = newCreateDto(CODE_DB_UPPER, COURSE_SECURITY, null, testTeacher.getId());
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(a, b)));
	}

	@Test
	@DisplayName("createAll: duplicate names in request (case-insensitive) -> IllegalArgumentException")
	void createAll_duplicateNamesInRequest_fails() {
		var a = newCreateDto(CODE_SEC_UPPER, COURSE_DATABASES, null, testTeacher.getId());
		var b = newCreateDto(CODE_DB_UPPER, COURSE_DATABASES, null, testTeacher.getId());
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(a, b)));
	}

	@Test
	@DisplayName("createAll: code already exists in DB (case-insensitive) -> IllegalArgumentException")
	void createAll_codeConflictInDb_fails() {
		var c = newCreateDto(CODE_SEC_UPPER, COURSE_DATABASES, null, testTeacher.getId());
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: name already exists in DB (case-insensitive) -> IllegalArgumentException")
	void createAll_nameConflictInDb_fails() {
		var c = newCreateDto(CODE_DB_UPPER, COURSE_SECURITY, null, testTeacher.getId());
		assertThrows(IllegalArgumentException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("createAll: non-existing teacher id -> EntityNotFoundException")
	void createAll_missingTeacher_fkFails() {
		var c = newCreateDto(CODE_ALG_UPPER, COURSE_ALG, null, NON_EXISTENT_ID);
		assertThrows(EntityNotFoundException.class, () -> courseService.createAll(List.of(c)));
	}

	@Test
	@DisplayName("findByIds: null/empty -> returns empty")
	void findByIds_nullOrEmpty_returnsEmpty() {
		assertTrue(courseService.findByIds(null).isEmpty());
		assertTrue(courseService.findByIds(List.of()).isEmpty());
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
	@DisplayName("updateSelf: happy path — updates name and normalizes blank description to null")
	void updateSelf_happyPath_success() {
		var patch = newUpdateSelf(courseSecurity.getId(), COURSE_DATABASES, DESC_BLANK);
		courseService.updateSelf(patch);

		var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();

		assertEquals(COURSE_DATABASES, reloaded.getName());
		assertNull(reloaded.getDescription(), "Blank description must become null");
	}

	@Test
	@DisplayName("updateSelf: null id in DTO -> ConstraintViolationException")
	void updateSelf_nullId_fails() {
		var patch = newUpdateSelf(null, COURSE_SECURITY, null);
		assertThrows(ConstraintViolationException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: non-existing id -> EntityNotFoundException")
	void updateSelf_notFound_fails() {
		var patch = newUpdateSelf(NON_EXISTENT_ID, COURSE_SECURITY, null);
		assertThrows(EntityNotFoundException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: no-op name (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopName_ok() {
		var patch = newUpdateSelf(courseSecurity.getId(), COURSE_SECURITY_LOWER, null);
		var updated = courseService.updateSelf(patch);
		assertEquals(COURSE_SECURITY, updated.getName());
	}

	@Test
	@DisplayName("updateSelf: change name to existing (case-insensitive) -> IllegalArgumentException")
	void updateSelf_changeNameToExisting_fails() {
		var patch = newUpdateSelf(courseSecurity.getId(), courseOperationSys.getName(), null);
		assertThrows(IllegalArgumentException.class, () -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateCodes: happy path — updates code for one course")
	void updateCodes_happyPath_success() {
		tempCourse = courseService
				.createAll(List.of(newCreateDto(TO_CREATE_CODE, TO_CREATE_NAME, null, testTeacher.getId())))
				.getFirst();

		var updatedCount = courseService.updateCodes(List.of(newUpdateCode(tempCourse.getId(), CODE_DB_UPPER)));

		assertEquals(ONE_COURSE, updatedCount);

		var reloaded = courseService.findByIds(List.of(tempCourse.getId())).getFirst();
		assertEquals(CODE_DB_UPPER, reloaded.getCode());
	}

	@Test
	@DisplayName("updateCodes: duplicate codes in request -> IllegalArgumentException")
	void updateCodes_duplicateCodesInRequest_fails() {
		var p1 = newUpdateCode(courseSecurity.getId(), CODE_DB_UPPER);
		var p2 = newUpdateCode(courseOperationSys.getId(), CODE_DB_UPPER);
		assertThrows(IllegalArgumentException.class, () -> courseService.updateCodes(List.of(p1, p2)));
	}

	@Test
	@DisplayName("updateCodes: code conflict with DB -> IllegalArgumentException")
	void updateCodes_codeConflictInDb_fails() {
		var p = newUpdateCode(courseSecurity.getId(), CODE_OS_UPPER);
		assertThrows(IllegalArgumentException.class, () -> courseService.updateCodes(List.of(p)));
	}

	@Test
	@DisplayName("updateCodes: no-op code -> returns 1 and unchanged")
	void updateCodes_noop_ok() {
		var count = courseService.updateCodes(List.of(newUpdateCode(courseSecurity.getId(), CODE_SEC_UPPER)));
		assertEquals(ONE_COURSE, count);

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
		assertEquals(0, courseService.updateCodes(List.of()));
	}

	@Test
	@DisplayName("updateCodes: null id -> ConstraintViolationException")
	void updateCodes_nullKey_fails() {
		var bad = newUpdateCode(null, CODE_SEC_UPPER);
		assertThrows(ConstraintViolationException.class, () -> courseService.updateCodes(List.of(bad)));
	}

	@Test
	@DisplayName("updateCodes: null code -> ConstraintViolationException")
	void updateCodes_nullValue_fails() {
		var bad = newUpdateCode(courseSecurity.getId(), null);
		assertThrows(ConstraintViolationException.class, () -> courseService.updateCodes(List.of(bad)));
	}

	@Test
	@DisplayName("updateCodes: bad code pattern after normalization -> ConstraintViolationException")
	void updateCodes_badPattern_fails() {
		var bad = newUpdateCode(courseSecurity.getId(), BAD_CODE);
		assertThrows(ConstraintViolationException.class, () -> courseService.updateCodes(List.of(bad)));
	}

	@Test
	@DisplayName("updateCodes: missing id with distinct code -> EntityNotFoundException")
	void updateCodes_missingId_distinctCode_fails() {
		var p1 = newUpdateCode(courseSecurity.getId(), CODE_SEC_UPPER);
		var p2 = newUpdateCode(NON_EXISTENT_ID, CODE_DB_UPPER);
		assertThrows(EntityNotFoundException.class, () -> courseService.updateCodes(List.of(p1, p2)));
	}

	@Test
	@DisplayName("addGroupsToCourse: happy path — adds unique groups, ignores nulls/duplicates")
	void addGroupsToCourse_happyPath_addsUnique() {
		courseService.addGroupsToCourse(courseSecurity.getId(),
				Arrays.asList(g1.getId(), g1.getId(), null, g2.getId()));

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
		int removed = courseService.removeGroupsFromCourse(courseOperationSys.getId(),
				Arrays.asList(g1.getId(), NON_EXISTENT_ID, null));

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
		var c = courseService
				.createAll(List.of(newCreateDto(TO_DELETE_CODE, TO_DELETE_NAME, null, testTeacher.getId())))
				.get(0);

		var res = courseService.deleteByIds(Arrays.asList(c.getId(), NON_EXISTENT_ID, null));
		assertEquals(Set.of(c.getId()), res.deletedIds(), "should delete existing id");
		assertEquals(Set.of(NON_EXISTENT_ID), res.notFoundIds(), "should report missing id");
	}
}
