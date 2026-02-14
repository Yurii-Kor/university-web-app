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
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
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
        DtoMapper.class,
        DuplicateGuard.class
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
    private Teacher otherTeacher;
    private StudyGroup g1, g2;

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
        // Teacher #1
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

        // Teacher #2 (для update teacher)
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

    // ---------------- CREATE ----------------

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
        assertThrows(EntityNotFoundException.class, () -> courseService.updateSelf(patch));
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
        assertThrows(IllegalArgumentException.class, () -> courseService.updateSelf(patch));
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
        assertThrows(IllegalArgumentException.class, () -> courseService.updateSelf(patch));
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

    // ---------------- DELETE ----------------

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
                .getFirst();

        var res = courseService.deleteByIds(Arrays.asList(c.getId(), NON_EXISTENT_ID, null));
        assertEquals(Set.of(c.getId()), res.deletedIds(), "should delete existing id");
        assertEquals(Set.of(NON_EXISTENT_ID), res.notFoundIds(), "should report missing id");
    }
}
