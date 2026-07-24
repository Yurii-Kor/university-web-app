package ua.foxminded.university.service.course;

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
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.course.dto.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.service.course.exception.CourseCreateException;
import ua.foxminded.university.service.course.exception.CourseSelfUpdateException;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	statements = {
		"""
		insert into app_user (id, email, password, role, first_name, last_name, enabled)
		values (1001, 'teacher@example.com', 'Abcd1234!', 'TEACHER', 'Ada', 'Lovelace',	true);
		""",
		"""
		insert into app_user (id, email, password, role, first_name, last_name, enabled)
		values (1002, 'teacher2@example.com', 'Abcd1234!', 'TEACHER', 'Alan', 'Turing',	true);
		""",
		"""
		insert into app_user (id, email, password, role, first_name, last_name, enabled)
		values (1003, 'student@example.com', 'Abcd1234!', 'STUDENT', 'Grace', 'Hopper', true);
		""",
		"""
		insert into teacher (id, academic_rank,	office)
		values (1001, 'PROFESSOR', 'A-101');
		""",
		"""
		insert into teacher (id, academic_rank,	office)
		values (1002, 'LECTURER', 'B-202');
		""",
		"""
		insert into groups (id,	name)
		values (2001, 'CS-201');
		""",
		"""
		insert into groups (id, name)
		values (2002, 'CS-202');
		""",
		"""
		insert into student (id, group_id, enrollment_year)
		values (1003, 2001,	2024);
		""",
		"""
		insert into courses (id, code, name, description, teacher_id)
		values (3001, 'CSE-OS-302',	'Operating Systems', '', 1001);
		""",
		"""
		insert into courses (id, code, name, description, teacher_id)
		values (3002, 'SEC-303', 'Security', '', 1001);
		""",
		"""
		insert into group_courses (group_id, course_id)
		values (2001, 3001);
		""",
		"""
		insert into group_courses (group_id, course_id)
		values (2002, 3001);
		""",
		"""
		insert into group_courses (group_id, course_id)
		values (2001, 3002);
		"""
	},
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CourseServiceTest {

	static final long TEST_TEACHER_ID = 1001L;
	static final long OTHER_TEACHER_ID = 1002L;
	static final long STUDENT_IN_G1_ID = 1003L;

	static final long GROUP_1_ID = 2001L;
	static final long GROUP_2_ID = 2002L;

	static final long COURSE_OS_ID = 3001L;
	static final long COURSE_SECURITY_ID = 3002L;

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

	static final String TO_DELETE_NAME = "To Delete";
	static final String TO_CREATE_NAME = "To Create";

	static final String DESC_A = "  Intro to algorithms ";
	static final String DESC_BLANK = "   ";

	static final long NON_EXISTENT_ID = 999_999L;

	static final int ONE_COURSE = 1;

	@Autowired
	private CourseService courseService;

	private CourseCreateDto newCreateDto(
			String code,
			String name,
			String description,
			Long teacherId) {

		return new CourseCreateDto(
				code,
				name,
				description,
				teacherId);
	}

	private CourseSelfUpdateDto newUpdateSelf(
			Long id,
			String code,
			String name) {

		return new CourseSelfUpdateDto(
				id,
				code,
				name,
				null);
	}

	private CourseSelfUpdateDto newUpdateSelf(
			Long id,
			String code,
			String name,
			Long teacherId) {

		return new CourseSelfUpdateDto(
				id,
				code,
				name,
				teacherId);
	}

	private CourseDescriptionUpdateDto newUpdateDescription(
			Long id,
			String description) {

		return new CourseDescriptionUpdateDto(
				id,
				description);
	}

	// ---------------- CREATE ----------------

	@Test
	@DisplayName("create: happy path — creates course with teacher and normalized name")
	void create_happyPath_success() {
		var dto = newCreateDto(
				CODE_ALG_UPPER,
				COURSE_ALG,
				DESC_A,
				TEST_TEACHER_ID);

		var createdCourse = courseService.create(dto);

		assertNotNull(createdCourse.getId());
		assertEquals(CODE_ALG_UPPER, createdCourse.getCode());
		assertEquals(COURSE_ALG.trim(), createdCourse.getName());
		assertEquals(
				TEST_TEACHER_ID,
				createdCourse.getTeacher().getId());
	}

	@Test
	@DisplayName("create: invalid code pattern (DTO) -> ConstraintViolationException")
	void create_badCode_fails() {
		var bad = newCreateDto(
				BAD_CODE,
				COURSE_ALG,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.create(bad));
	}

	@Test
	@DisplayName("create: null teacherId in DTO -> ConstraintViolationException")
	void create_nullTeacher_fails() {
		var course = newCreateDto(
				CODE_ALG_UPPER,
				COURSE_SECURITY,
				null,
				null);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.create(course));
	}

	@Test
	@DisplayName("create: code already exists in DB (case-insensitive) -> CourseCreateException")
	void create_codeConflictInDb_fails() {
		var course = newCreateDto(
				CODE_SEC_UPPER,
				COURSE_DATABASES,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(course));
	}

	@Test
	@DisplayName("create: name already exists in DB (case-insensitive) -> CourseCreateException")
	void create_nameConflictInDb_fails() {
		var course = newCreateDto(
				CODE_DB_UPPER,
				COURSE_SECURITY,
				null,
				TEST_TEACHER_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(course));
	}

	@Test
	@DisplayName("create: non-existing teacher id -> CourseCreateException")
	void create_missingTeacher_fkFails() {
		var course = newCreateDto(
				CODE_ALG_UPPER,
				COURSE_ALG,
				null,
				NON_EXISTENT_ID);

		assertThrows(
				CourseCreateException.class,
				() -> courseService.create(course));
	}

	// ---------------- FIND ----------------

	@Test
	@DisplayName("findByIds: null/empty -> returns empty")
	void findByIds_nullOrEmpty_returnsEmpty() {
		assertTrue(courseService.findByIds(null).isEmpty());
		assertTrue(courseService.findByIds(List.of()).isEmpty());
	}

	@Test
	@DisplayName("findByIds: contains nulls and duplicates -> unique non-null results")
	void findByIds_nullsAndDuplicates_ok() {
		var ids = Arrays.asList(
				COURSE_SECURITY_ID,
				null,
				COURSE_SECURITY_ID);

		var result = courseService.findByIds(ids);

		assertEquals(ONE_COURSE, result.size());
		assertEquals(
				COURSE_SECURITY_ID,
				result.getFirst().getId());
	}

	@Test
	@DisplayName("listCourseCardsForAdmin: returns all course cards")
	void listCourseCardsForAdmin_returnsAll() {
		var cardsPage = courseService.listCourseCardsForAdmin(
				PageRequest.of(0, 10));

		assertNotNull(cardsPage);
		assertFalse(
				cardsPage.isEmpty(),
				"admin list must not be empty");

		var ids = cardsPage.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(COURSE_OS_ID),
				"must contain seeded OS course");

		assertTrue(
				ids.contains(COURSE_SECURITY_ID),
				"must contain seeded Security course");
	}

	@Test
	@DisplayName("listCourseCardsForTeacher: returns only teacher courses")
	void listCourseCardsForTeacher_existingTeacher_returnsOnlyOwnCourses() {
		var otherTeacherCourse = courseService.create(
				newCreateDto(
						TO_CREATE_CODE,
						TO_CREATE_NAME,
						null,
						OTHER_TEACHER_ID));

		var cardsPage = courseService.listCourseCardsForTeacher(
				TEST_TEACHER_ID,
				PageRequest.of(0, 10));

		assertNotNull(cardsPage);
		assertFalse(cardsPage.isEmpty());

		var ids = cardsPage.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(COURSE_OS_ID),
				"must contain OS course of testTeacher");

		assertTrue(
				ids.contains(COURSE_SECURITY_ID),
				"must contain Security course of testTeacher");

		assertFalse(
				ids.contains(otherTeacherCourse.getId()),
				"must not contain course of another teacher");
	}

	@Test
	@DisplayName("listCourseCardsForTeacher: non-existing teacher id -> empty list")
	void listCourseCardsForTeacher_notFound_returnsEmpty() {
		var cardsPage = courseService.listCourseCardsForTeacher(
				NON_EXISTENT_ID,
				PageRequest.of(0, 10));

		assertNotNull(cardsPage);
		assertTrue(
				cardsPage.isEmpty(),
				"non-existing teacher must return empty list");
	}

	@Test
	@DisplayName("listCourseCardsForStudent: returns courses available for student's group")
	void listCourseCardsForStudent_existingStudent_returnsGroupCourses() {
		var cardsPage = courseService.listCourseCardsForStudent(
				STUDENT_IN_G1_ID,
				PageRequest.of(0, 10));

		assertNotNull(cardsPage);
		assertFalse(cardsPage.isEmpty());

		var ids = cardsPage.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(COURSE_OS_ID),
				"student in g1 must see OS course");

		assertTrue(
				ids.contains(COURSE_SECURITY_ID),
				"student in g1 must see Security course");
	}

	@Test
	@DisplayName("listCourseCardsForStudent: non-existing student id -> empty list")
	void listCourseCardsForStudent_notFound_returnsEmpty() {
		var cardsPage = courseService.listCourseCardsForStudent(
				NON_EXISTENT_ID,
				PageRequest.of(0, 10));

		assertNotNull(cardsPage);
		assertTrue(
				cardsPage.isEmpty(),
				"non-existing student must return empty list");
	}

	@Test
	@DisplayName("getCourseGroupsPage: existing course id -> returns page view with header and assigned/available groups")
	void getCourseGroupsPage_existingId_returnsPage() {
		var page = courseService.getCourseGroupsView(
				COURSE_SECURITY_ID);

		assertNotNull(
				page,
				"page must not be null");

		assertNotNull(
				page.course(),
				"course header must not be null");

		assertEquals(
				GROUP_1_ID,
				page.assigned().getFirst().id(),
				"assigned groups must match seeded data");

		assertEquals(
				GROUP_2_ID,
				page.available().getFirst().id(),
				"available groups must match seeded data");
	}

	@Test
	@DisplayName("getCourseGroupsPage: non-existing course id -> EntityNotFoundException")
	void getCourseGroupsPage_notFound_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.getCourseGroupsView(NON_EXISTENT_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}

	// ---------------- UPDATE SELF ----------------

	@Test
	@DisplayName("updateSelf: happy path — updates code & name")
	void updateSelf_happyPath_success() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				CODE_DB_UPPER,
				COURSE_DATABASES);

		courseService.updateSelf(patch);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(CODE_DB_UPPER, reloaded.getCode());
		assertEquals(COURSE_DATABASES, reloaded.getName());

		assertEquals(
				TEST_TEACHER_ID,
				reloaded.getTeacher().getId(),
				"teacher must stay unchanged when teacherId is null");
	}

	@Test
	@DisplayName("updateSelf: change teacher -> ok")
	void updateSelf_changeTeacher_success() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				null,
				null,
				OTHER_TEACHER_ID);

		courseService.updateSelf(patch);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				OTHER_TEACHER_ID,
				reloaded.getTeacher().getId());

		assertEquals(
				CODE_SEC_UPPER,
				reloaded.getCode(),
				"code must remain unchanged");

		assertEquals(
				COURSE_SECURITY,
				reloaded.getName(),
				"name must remain unchanged");
	}

	@Test
	@DisplayName("updateSelf: change teacher to non-existing -> EntityNotFoundException")
	void updateSelf_changeTeacherToMissing_fails() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				null,
				null,
				NON_EXISTENT_ID);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: teacherId = 0 -> ConstraintViolationException (@Positive)")
	void updateSelf_teacherIdZero_fails() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				null,
				null,
				0L);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: null id in DTO -> ConstraintViolationException")
	void updateSelf_nullId_fails() {
		var patch = newUpdateSelf(
				null,
				null,
				COURSE_SECURITY);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: non-existing id -> EntityNotFoundException")
	void updateSelf_notFound_fails() {
		var patch = newUpdateSelf(
				NON_EXISTENT_ID,
				null,
				COURSE_SECURITY);

		assertThrows(
				EntityNotFoundException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: no-op code (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopCode_ok() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				CODE_SEC_UPPER,
				null);

		var updated = courseService.updateSelf(patch);

		assertEquals(
				CODE_SEC_UPPER,
				updated.getCode());
	}

	@Test
	@DisplayName("updateSelf: change code to existing in DB -> IllegalArgumentException")
	void updateSelf_changeCodeToExisting_fails() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				CODE_OS_UPPER,
				null);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: bad code pattern -> ConstraintViolationException")
	void updateSelf_badCodePattern_fails() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				BAD_CODE,
				null);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateSelf(patch));
	}

	@Test
	@DisplayName("updateSelf: no-op name (case-insensitive equal) -> ok and unchanged")
	void updateSelf_noopName_ok() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				null,
				COURSE_SECURITY_LOWER);

		var updated = courseService.updateSelf(patch);

		assertEquals(
				COURSE_SECURITY,
				updated.getName());
	}

	@Test
	@DisplayName("updateSelf: change name to existing (case-insensitive) -> IllegalArgumentException")
	void updateSelf_changeNameToExisting_fails() {
		var patch = newUpdateSelf(
				COURSE_SECURITY_ID,
				null,
				COURSE_OS);

		assertThrows(
				CourseSelfUpdateException.class,
				() -> courseService.updateSelf(patch));
	}

	// ---------------- UPDATE DESCRIPTION ----------------

	@Test
	@DisplayName("updateDescription: happy path — updates description and trims")
	void updateDescription_happyPath_success() {
		var patch = newUpdateDescription(
				COURSE_SECURITY_ID,
				DESC_A);

		courseService.updateDescription(patch);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				DESC_A.trim(),
				reloaded.getDescription());
	}

	@Test
	@DisplayName("updateDescription: blank -> normalizes to null")
	void updateDescription_blankToNull_success() {
		courseService.updateDescription(
				newUpdateDescription(
						COURSE_SECURITY_ID,
						"Some description"));

		courseService.updateDescription(
				newUpdateDescription(
						COURSE_SECURITY_ID,
						DESC_BLANK));

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertNull(
				reloaded.getDescription(),
				"Blank description must become null");
	}

	@Test
	@DisplayName("updateDescription: null description -> no-op (keeps existing)")
	void updateDescription_nullDescription_noop() {
		courseService.updateDescription(
				newUpdateDescription(
						COURSE_SECURITY_ID,
						"Existing"));

		courseService.updateDescription(
				newUpdateDescription(
						COURSE_SECURITY_ID,
						null));

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				"Existing",
				reloaded.getDescription(),
				"Null description must not change existing value");
	}

	@Test
	@DisplayName("updateDescription: null id -> ConstraintViolationException")
	void updateDescription_nullId_fails() {
		var patch = newUpdateDescription(
				null,
				"abc");

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateDescription(patch));
	}

	@Test
	@DisplayName("updateDescription: non-existing id -> EntityNotFoundException")
	void updateDescription_notFound_fails() {
		var patch = newUpdateDescription(
				NON_EXISTENT_ID,
				"abc");

		assertThrows(
				EntityNotFoundException.class,
				() -> courseService.updateDescription(patch));
	}

	@Test
	@DisplayName("updateDescription: too long -> ConstraintViolationException")
	void updateDescription_tooLong_fails() {
		var tooLong = "A".repeat(10_001);

		var patch = newUpdateDescription(
				COURSE_SECURITY_ID,
				tooLong);

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateDescription(patch));
	}

	// ---------------- GROUPS ----------------

	@Test
	@DisplayName("addGroupToCourse: adds group when not attached -> returns Optional.of(groupId) and attaches")
	void addGroupToCourse_adds_whenNotAttached() {
	    var added = courseService.addGroupToCourse(
	            COURSE_SECURITY_ID,
	            GROUP_2_ID);

	    assertTrue(
	            added.isPresent(),
	            "must return groupId when added");

	    assertEquals(
	            GROUP_2_ID,
	            added.get());

	    var groupsView = courseService.getCourseGroupsView(
	            COURSE_SECURITY_ID);

	    var assignedGroupIds = groupsView.assigned()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertTrue(
	            assignedGroupIds.containsAll(
	                    List.of(GROUP_1_ID, GROUP_2_ID)),
	            "course must reference both groups");

	    assertTrue(
	            groupsView.available().isEmpty(),
	            "no groups must remain available");
	}

	@Test
	@DisplayName("addGroupToCourse: group already attached -> returns Optional.empty and changes nothing")
	void addGroupToCourse_alreadyAttached_returnsEmpty() {
	    var added = courseService.addGroupToCourse(
	            COURSE_SECURITY_ID,
	            GROUP_1_ID);

	    assertTrue(
	            added.isEmpty(),
	            "must be empty when already attached");

	    var groupsView = courseService.getCourseGroupsView(
	            COURSE_SECURITY_ID);

	    var assignedGroupIds = groupsView.assigned()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertEquals(
	            List.of(GROUP_1_ID),
	            assignedGroupIds,
	            "course must still have only the originally assigned group");

	    var availableGroupIds = groupsView.available()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertEquals(
	            List.of(GROUP_2_ID),
	            availableGroupIds,
	            "second group must remain available");
	}

	@Test
	@DisplayName("addGroupToCourse: non-existing course -> EntityNotFoundException")
	void addGroupToCourse_courseNotFound_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.addGroupToCourse(
						NON_EXISTENT_ID,
						GROUP_1_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("removeGroupFromCourse: removes attached group -> returns Optional.of(groupId) and detaches")
	void removeGroupFromCourse_removes_whenAttached() {
	    var removed = courseService.removeGroupFromCourse(
	            COURSE_OS_ID,
	            GROUP_1_ID);

	    assertTrue(
	            removed.isPresent(),
	            "must return groupId when removed");

	    assertEquals(
	            GROUP_1_ID,
	            removed.get());

	    var groupsView = courseService.getCourseGroupsView(
	            COURSE_OS_ID);

	    var assignedGroupIds = groupsView.assigned()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertFalse(
	            assignedGroupIds.contains(GROUP_1_ID),
	            "g1 must be removed");

	    assertTrue(
	            assignedGroupIds.contains(GROUP_2_ID),
	            "g2 must remain assigned");

	    var availableGroupIds = groupsView.available()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertTrue(
	            availableGroupIds.contains(GROUP_1_ID),
	            "removed group must become available");
	}

	@Test
	@DisplayName("removeGroupFromCourse: group not attached -> returns Optional.empty and changes nothing")
	void removeGroupFromCourse_notAttached_returnsEmpty() {
	    var removed = courseService.removeGroupFromCourse(
	            COURSE_SECURITY_ID,
	            GROUP_2_ID);

	    assertTrue(
	            removed.isEmpty(),
	            "must be empty when group is not attached");

	    var groupsView = courseService.getCourseGroupsView(
	            COURSE_SECURITY_ID);

	    var assignedGroupIds = groupsView.assigned()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertEquals(
	            List.of(GROUP_1_ID),
	            assignedGroupIds,
	            "only g1 must remain assigned");

	    var availableGroupIds = groupsView.available()
	            .stream()
	            .map(group -> group.id())
	            .toList();

	    assertEquals(
	            List.of(GROUP_2_ID),
	            availableGroupIds,
	            "g2 must remain available");
	}

	@Test
	@DisplayName("removeGroupFromCourse: non-existing course -> EntityNotFoundException")
	void removeGroupFromCourse_courseNotFound_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.removeGroupFromCourse(
						NON_EXISTENT_ID,
						GROUP_1_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}

	// ---------------- DELETE ----------------

	@Test
	@DisplayName("delete: existing id -> deletes course")
	void delete_existingId_deletes() {
		var courseToDelete = courseService.create(
				newCreateDto(
						TO_DELETE_CODE,
						TO_DELETE_NAME,
						null,
						TEST_TEACHER_ID));

		Long id = courseToDelete.getId();

		assertNotNull(id);

		courseService.delete(id);

		assertTrue(
				courseService.findByIds(List.of(id)).isEmpty(),
				"course must be deleted");
	}

	@Test
	@DisplayName("delete: non-existing id -> EntityNotFoundException")
	void delete_notFound_throwsEntityNotFound() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.delete(NON_EXISTENT_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}
}