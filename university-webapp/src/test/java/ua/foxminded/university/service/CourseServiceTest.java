package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.exception.course.CourseCreateException;
import ua.foxminded.university.service.exception.course.CourseSelfUpdateException;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({
        TestcontainersConfiguration.class,
        CourseService.class,
        ValidatorConfig.class,
        EntityValidatior.class,
        TestDataInitializer.class,
        DtoMapper.class
})
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

    static final String TO_DELETE_NAME = "To Delete";
    static final String TO_CREATE_NAME = "To Create";

    static final String DESC_A = "  Intro to algorithms ";
    static final String DESC_BLANK = "   ";

    static final long NON_EXISTENT_ID = 999_999L;

    static final int ONE_GROUP = 1;
    static final int TWO_GROUPS = 2;
    static final int ONE_COURSE = 1;

    @Autowired
    private CourseService courseService;

    @Autowired
    private TestDataInitializer initializer;

    private Course courseSecurity, courseOperationSys, tempCourse;
    private Teacher testTeacher;
    private Teacher otherTeacher;
    private StudyGroup g1, g2;
    private Student studentInG1;

    private CourseCreateDto newCreateDto(String code, String name, String desc, Long teacherId) {
        return new CourseCreateDto(code, name, desc, teacherId);
    }

    private CourseSelfUpdateDto newUpdateSelf(Long id, String code, String name) {
        return new CourseSelfUpdateDto(id, code, name, null);
    }

    private CourseSelfUpdateDto newUpdateSelf(Long id, String code, String name, Long teacherId) {
        return new CourseSelfUpdateDto(id, code, name, teacherId);
    }

    private CourseDescriptionUpdateDto newUpdateDescription(Long id, String description) {
        return new CourseDescriptionUpdateDto(id, description);
    }

    @BeforeAll
    void setup() {
        var testUser1 = AppUser.builder()
                .email("teacher@example.com")
                .password("Abcd1234!")
                .role(UserRole.TEACHER)
                .firstName("Ada")
                .lastName("Lovelace")
                .enabled(true)
                .build();

        testTeacher = initializer
                .persistAll(Teacher.builder()
                        .user(testUser1)
                        .academicRank(AcademicRank.PROFESSOR)
                        .office("A-101")
                        .build())
                .getFirst();

        var testUser2 = AppUser.builder()
                .email("teacher2@example.com")
                .password("Abcd1234!")
                .role(UserRole.TEACHER)
                .firstName("Alan")
                .lastName("Turing")
                .enabled(true)
                .build();

        otherTeacher = initializer
                .persistAll(Teacher.builder()
                        .user(testUser2)
                        .academicRank(AcademicRank.LECTURER)
                        .office("B-202")
                        .build())
                .getFirst();

        g1 = initializer.persistAll(StudyGroup.builder().name("CS-201").build()).getFirst();
        g2 = initializer.persistAll(StudyGroup.builder().name("CS-202").build()).getFirst();

        courseOperationSys = initializer.persistAll(Course.builder()
                        .code(CODE_OS_UPPER)
                        .name(COURSE_OS)
                        .description("")
                        .teacher(Teacher.builder().id(testTeacher.getId()).build())
                        .groups(new LinkedHashSet<>(Arrays.asList(
                                StudyGroup.builder().id(g1.getId()).build(),
                                StudyGroup.builder().id(g2.getId()).build()
                        )))
                        .build())
                .getFirst();
        
        var studentUser = AppUser.builder()
                .email("student@example.com")
                .password("Abcd1234!")
                .role(UserRole.STUDENT)
                .firstName("Grace")
                .lastName("Hopper")
                .enabled(true)
                .build();
        
        studentInG1 = initializer
                .persistAll(Student.builder()
                        .user(studentUser)
                        .group(g1)
                        .enrollmentYear(2024)
                        .build())
                .getFirst();
    }

    @BeforeEach
    void setupCourseSecurity() {
        courseSecurity = courseService.create(newCreateDto(CODE_SEC_UPPER, COURSE_SECURITY, "", testTeacher.getId()));
        
        courseService.addGroupToCourse(courseSecurity.getId(), g1.getId());
    }

    @AfterEach
    void cleanup() {
        Optional.ofNullable(tempCourse)
                .map(Course::getId)
                .ifPresent(id -> safeDelete(id));

        Optional.ofNullable(courseSecurity)
                .map(Course::getId)
                .ifPresent(id -> safeDelete(id));

        tempCourse = null;
        courseSecurity = null;
    }

	private void safeDelete(Long id) {
		try {
			courseService.delete(id);
		} catch (EntityNotFoundException ignored) {}
	}

    // ---------------- CREATE ----------------

    @Test
    @DisplayName("create: happy path — creates course with teacher and normalized name")
    void create_happyPath_success() {
        var dto = newCreateDto(CODE_ALG_UPPER, COURSE_ALG, DESC_A, testTeacher.getId());

        tempCourse = courseService.create(dto);

        assertNotNull(tempCourse.getId());
        assertEquals(CODE_ALG_UPPER, tempCourse.getCode());
        assertEquals(COURSE_ALG.trim(), tempCourse.getName());
        assertEquals(testTeacher.getId(), tempCourse.getTeacher().getId());
    }

    @Test
    @DisplayName("create: invalid code pattern (DTO) -> ConstraintViolationException")
    void create_badCode_fails() {
        var bad = newCreateDto(BAD_CODE, COURSE_ALG, null, testTeacher.getId());
        assertThrows(ConstraintViolationException.class, () -> courseService.create(bad));
    }

    @Test
    @DisplayName("create: null teacherId in DTO -> ConstraintViolationException")
    void create_nullTeacher_fails() {
        var c = newCreateDto(CODE_ALG_UPPER, COURSE_SECURITY, null, null);
        assertThrows(ConstraintViolationException.class, () -> courseService.create(c));
    }

    @Test
    @DisplayName("create: code already exists in DB (case-insensitive) -> CourseCreateException")
    void create_codeConflictInDb_fails() {
        var c = newCreateDto(CODE_SEC_UPPER, COURSE_DATABASES, null, testTeacher.getId());
        assertThrows(CourseCreateException.class, () -> courseService.create(c));
    }

    @Test
    @DisplayName("create: name already exists in DB (case-insensitive) -> CourseCreateException")
    void create_nameConflictInDb_fails() {
        var c = newCreateDto(CODE_DB_UPPER, COURSE_SECURITY, null, testTeacher.getId());
        assertThrows(CourseCreateException.class, () -> courseService.create(c));
    }

    @Test
    @DisplayName("create: non-existing teacher id -> CourseCreateException")
    void create_missingTeacher_fkFails() {
        var c = newCreateDto(CODE_ALG_UPPER, COURSE_ALG, null, NON_EXISTENT_ID);
        assertThrows(CourseCreateException.class, () -> courseService.create(c));
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
        var ids = Arrays.asList(courseSecurity.getId(), null, courseSecurity.getId());
        var res = courseService.findByIds(ids);
        assertEquals(ONE_COURSE, res.size());
        assertEquals(courseSecurity.getId(), res.getFirst().getId());
    }
    
    @Test
    @DisplayName("listCourseCardsForAdmin: returns all course cards")
    void listCourseCardsForAdmin_returnsAll() {
        var cardsPage = courseService.listCourseCardsForAdmin(PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(cardsPage.isEmpty(), "admin list must not be empty");

        var ids = cardsPage.getContent().stream().map(c -> c.id()).toList();

        assertTrue(ids.contains(courseOperationSys.getId()), "must contain seeded OS course");
        assertTrue(ids.contains(courseSecurity.getId()), "must contain seeded Security course");
    }
    
    @Test
    @DisplayName("listCourseCardsForTeacher: returns only teacher courses")
    void listCourseCardsForTeacher_existingTeacher_returnsOnlyOwnCourses() {
        tempCourse = courseService.create(newCreateDto(TO_CREATE_CODE, TO_CREATE_NAME, null, otherTeacher.getId()));

        var cardsPage = courseService.listCourseCardsForTeacher(testTeacher.getId(), PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(cardsPage.isEmpty());

        var ids = cardsPage.getContent().stream().map(c -> c.id()).toList();

        assertTrue(ids.contains(courseOperationSys.getId()), "must contain OS course of testTeacher");
        assertTrue(ids.contains(courseSecurity.getId()), "must contain Security course of testTeacher");
        assertFalse(ids.contains(tempCourse.getId()), "must not contain course of another teacher");
    }
    
    @Test
    @DisplayName("listCourseCardsForTeacher: non-existing teacher id -> empty list")
    void listCourseCardsForTeacher_notFound_returnsEmpty() {
        var cardsPage = courseService.listCourseCardsForTeacher(NON_EXISTENT_ID, PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertTrue(cardsPage.isEmpty(), "non-existing teacher must return empty list");
    }
    
    @Test
    @DisplayName("listCourseCardsForStudent: returns courses available for student's group")
    void listCourseCardsForStudent_existingStudent_returnsGroupCourses() {
        var cardsPage = courseService.listCourseCardsForStudent(studentInG1.getId(), PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(cardsPage.isEmpty());

        var ids = cardsPage.getContent().stream().map(c -> c.id()).toList();

        assertTrue(ids.contains(courseOperationSys.getId()), "student in g1 must see OS course");
        assertTrue(ids.contains(courseSecurity.getId()), "student in g1 must see Security course");
    }
    
    @Test
    @DisplayName("listCourseCardsForStudent: non-existing student id -> empty list")
    void listCourseCardsForStudent_notFound_returnsEmpty() {
        var cardsPage = courseService.listCourseCardsForStudent(NON_EXISTENT_ID, PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertTrue(cardsPage.isEmpty(), "non-existing student must return empty list");
    }
    
    @Test
    @DisplayName("getCourseGroupsPage: existing course id -> returns page view with header and assigned/available groups")
    void getCourseGroupsPage_existingId_returnsPage() {
        var page = courseService.getCourseGroupsView(courseSecurity.getId());

        assertNotNull(page, "page must not be null");
        assertNotNull(page.course(), "course header must not be null");

        assertEquals(g1.getId(), page.assigned().getFirst().id(), "assigned groups must match seeded data");
        assertEquals(g2.getId(), page.available().getFirst().id(), "no available groups must match seeded data");
    }

    @Test
    @DisplayName("getCourseGroupsPage: non-existing course id -> EntityNotFoundException")
    void getCourseGroupsPage_notFound_fails() {
        var ex = assertThrows(EntityNotFoundException.class,
                () -> courseService.getCourseGroupsView(NON_EXISTENT_ID));

        assertEquals("Course not found: id=" + NON_EXISTENT_ID, ex.getMessage());
    }

    // ---------------- UPDATE SELF ----------------

    @Test
    @DisplayName("updateSelf: happy path — updates code & name")
    void updateSelf_happyPath_success() {
        var patch = newUpdateSelf(courseSecurity.getId(), CODE_DB_UPPER, COURSE_DATABASES);
        courseService.updateSelf(patch);

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();

        assertEquals(CODE_DB_UPPER, reloaded.getCode());
        assertEquals(COURSE_DATABASES, reloaded.getName());
        assertEquals(testTeacher.getId(), reloaded.getTeacher().getId(), "teacher must stay unchanged when teacherId is null");
    }

    @Test
    @DisplayName("updateSelf: change teacher -> ok")
    void updateSelf_changeTeacher_success() {
        var patch = newUpdateSelf(courseSecurity.getId(), null, null, otherTeacher.getId());

        courseService.updateSelf(patch);

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        assertEquals(otherTeacher.getId(), reloaded.getTeacher().getId());
        assertEquals(CODE_SEC_UPPER, reloaded.getCode(), "code must remain unchanged");
        assertEquals(COURSE_SECURITY, reloaded.getName(), "name must remain unchanged");
    }

    @Test
    @DisplayName("updateSelf: change teacher to non-existing -> EntityNotFoundException")
    void updateSelf_changeTeacherToMissing_fails() {
        var patch = newUpdateSelf(courseSecurity.getId(), null, null, NON_EXISTENT_ID);
        assertThrows(CourseSelfUpdateException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: teacherId = 0 -> ConstraintViolationException (@Positive)")
    void updateSelf_teacherIdZero_fails() {
        var patch = newUpdateSelf(courseSecurity.getId(), null, null, 0L);
        assertThrows(ConstraintViolationException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: null id in DTO -> ConstraintViolationException")
    void updateSelf_nullId_fails() {
        var patch = newUpdateSelf(null, null, COURSE_SECURITY);
        assertThrows(ConstraintViolationException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: non-existing id -> EntityNotFoundException")
    void updateSelf_notFound_fails() {
        var patch = newUpdateSelf(NON_EXISTENT_ID, null, COURSE_SECURITY);
        assertThrows(EntityNotFoundException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: no-op code (case-insensitive equal) -> ok and unchanged")
    void updateSelf_noopCode_ok() {
        var patch = newUpdateSelf(courseSecurity.getId(), CODE_SEC_UPPER, null);

        var updated = courseService.updateSelf(patch);

        assertEquals(CODE_SEC_UPPER, updated.getCode());
    }

    @Test
    @DisplayName("updateSelf: change code to existing in DB -> IllegalArgumentException")
    void updateSelf_changeCodeToExisting_fails() {
        var patch = newUpdateSelf(courseSecurity.getId(), CODE_OS_UPPER, null);
        assertThrows(CourseSelfUpdateException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: bad code pattern -> ConstraintViolationException")
    void updateSelf_badCodePattern_fails() {
        var patch = newUpdateSelf(courseSecurity.getId(), BAD_CODE, null);
        assertThrows(ConstraintViolationException.class, () -> courseService.updateSelf(patch));
    }

    @Test
    @DisplayName("updateSelf: no-op name (case-insensitive equal) -> ok and unchanged")
    void updateSelf_noopName_ok() {
        var patch = newUpdateSelf(courseSecurity.getId(), null, COURSE_SECURITY_LOWER);
        var updated = courseService.updateSelf(patch);
        assertEquals(COURSE_SECURITY, updated.getName());
    }

    @Test
    @DisplayName("updateSelf: change name to existing (case-insensitive) -> IllegalArgumentException")
    void updateSelf_changeNameToExisting_fails() {
        var patch = newUpdateSelf(courseSecurity.getId(), null, courseOperationSys.getName());
        assertThrows(CourseSelfUpdateException.class, () -> courseService.updateSelf(patch));
    }

    // ---------------- UPDATE DESCRIPTION ----------------

    @Test
    @DisplayName("updateDescription: happy path — updates description and trims")
    void updateDescription_happyPath_success() {
        var patch = newUpdateDescription(courseSecurity.getId(), DESC_A);

        courseService.updateDescription(patch);

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        assertEquals(DESC_A.trim(), reloaded.getDescription());
    }

    @Test
    @DisplayName("updateDescription: blank -> normalizes to null")
    void updateDescription_blankToNull_success() {
        courseService.updateDescription(newUpdateDescription(courseSecurity.getId(), "Some description"));

        courseService.updateDescription(newUpdateDescription(courseSecurity.getId(), DESC_BLANK));

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        assertNull(reloaded.getDescription(), "Blank description must become null");
    }

    @Test
    @DisplayName("updateDescription: null description -> no-op (keeps existing)")
    void updateDescription_nullDescription_noop() {
        courseService.updateDescription(newUpdateDescription(courseSecurity.getId(), "Existing"));

        courseService.updateDescription(newUpdateDescription(courseSecurity.getId(), null));

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        assertEquals("Existing", reloaded.getDescription(), "Null description must not change existing value");
    }

    @Test
    @DisplayName("updateDescription: null id -> ConstraintViolationException")
    void updateDescription_nullId_fails() {
        var patch = newUpdateDescription(null, "abc");
        assertThrows(ConstraintViolationException.class, () -> courseService.updateDescription(patch));
    }

    @Test
    @DisplayName("updateDescription: non-existing id -> EntityNotFoundException")
    void updateDescription_notFound_fails() {
        var patch = newUpdateDescription(NON_EXISTENT_ID, "abc");
        assertThrows(EntityNotFoundException.class, () -> courseService.updateDescription(patch));
    }

    @Test
    @DisplayName("updateDescription: too long -> ConstraintViolationException")
    void updateDescription_tooLong_fails() {
        var tooLong = "A".repeat(10_001);
        var patch = newUpdateDescription(courseSecurity.getId(), tooLong);
        assertThrows(ConstraintViolationException.class, () -> courseService.updateDescription(patch));
    }

 // ---------------- GROUPS ----------------

    @Test
    @DisplayName("addGroupToCourse: adds group when not attached -> returns Optional.of(groupId) and attaches")
    void addGroupToCourse_adds_whenNotAttached() {
        var added = courseService.addGroupToCourse(courseSecurity.getId(), g2.getId());

        assertTrue(added.isPresent(), "must return groupId when added");
        assertEquals(g2.getId(), added.get());

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        var ids = reloaded.getGroups().stream().map(StudyGroup::getId).toList();

        assertTrue(ids.containsAll(List.of(g1.getId(), g2.getId())), "course must reference both groups");
    }

    @Test
    @DisplayName("addGroupToCourse: group already attached -> returns Optional.empty and changes nothing")
    void addGroupToCourse_alreadyAttached_returnsEmpty() {
        var added = courseService.addGroupToCourse(courseSecurity.getId(), g1.getId());
        assertTrue(added.isEmpty(), "must be empty when already attached");

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        var ids = reloaded.getGroups().stream().map(StudyGroup::getId).toList();

        assertEquals(1, ids.size(), "still must have only one group");
        assertTrue(ids.contains(g1.getId()));
    }

    @Test
    @DisplayName("addGroupToCourse: non-existing course -> EntityNotFoundException")
    void addGroupToCourse_courseNotFound_fails() {
        var ex = assertThrows(EntityNotFoundException.class,
                () -> courseService.addGroupToCourse(NON_EXISTENT_ID, g1.getId()));

        assertEquals("Course not found: id=" + NON_EXISTENT_ID, ex.getMessage());
    }

    @Test
    @DisplayName("removeGroupFromCourse: removes attached group -> returns Optional.of(groupId) and detaches")
    void removeGroupFromCourse_removes_whenAttached() {
        var removed = courseService.removeGroupFromCourse(courseOperationSys.getId(), g1.getId());

        assertTrue(removed.isPresent(), "must return groupId when removed");
        assertEquals(g1.getId(), removed.get());

        var reloaded = courseService.findByIds(List.of(courseOperationSys.getId())).getFirst();
        var ids = reloaded.getGroups().stream().map(StudyGroup::getId).toList();

        assertFalse(ids.contains(g1.getId()), "g1 must be removed");
        assertTrue(ids.contains(g2.getId()), "g2 must remain");
    }

    @Test
    @DisplayName("removeGroupFromCourse: group not attached -> returns Optional.empty and changes nothing")
    void removeGroupFromCourse_notAttached_returnsEmpty() {
        var removed = courseService.removeGroupFromCourse(courseSecurity.getId(), g2.getId());
        assertTrue(removed.isEmpty(), "must be empty when group is not attached");

        var reloaded = courseService.findByIds(List.of(courseSecurity.getId())).getFirst();
        var ids = reloaded.getGroups().stream().map(StudyGroup::getId).toList();

        assertEquals(1, ids.size());
        assertTrue(ids.contains(g1.getId()));
    }

    @Test
    @DisplayName("removeGroupFromCourse: non-existing course -> EntityNotFoundException")
    void removeGroupFromCourse_courseNotFound_fails() {
        var ex = assertThrows(EntityNotFoundException.class,
                () -> courseService.removeGroupFromCourse(NON_EXISTENT_ID, g1.getId()));

        assertEquals("Course not found: id=" + NON_EXISTENT_ID, ex.getMessage());
    }

 // ---------------- DELETE ----------------

    @Test
    @DisplayName("delete: existing id -> deletes course")
    void delete_existingId_deletes() {
        tempCourse = courseService.create(
                newCreateDto(TO_DELETE_CODE, TO_DELETE_NAME, null, testTeacher.getId())
        );
        Long id = tempCourse.getId();
        assertNotNull(id);

        courseService.delete(id);

        assertTrue(courseService.findByIds(List.of(id)).isEmpty(), "course must be deleted");
    }

    @Test
    @DisplayName("delete: non-existing id -> EntityNotFoundException")
    void delete_notFound_throwsEntityNotFound() {
        var ex = assertThrows(EntityNotFoundException.class, () -> courseService.delete(NON_EXISTENT_ID));
        assertEquals("Course not found: id=" + NON_EXISTENT_ID, ex.getMessage());
    }
}
