package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.service.dto.request.teacher.TeacherCreateDto;
import ua.foxminded.university.service.dto.request.teacher.TeacherSelfUpdateDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.projectionmapper.DateTimeMapper;
import ua.foxminded.university.service.util.projectionmapper.DeletedTeacherCardMapper;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({
    TestcontainersConfiguration.class,
    TeacherService.class,
    AppUserService.class,
    ValidatorConfig.class,
	EntityValidatior.class,
	PasswordPolicy.class,
	PasswordEncoderConfig.class,
	DtoMapper.class,
	DuplicateGuard.class,
	TestDataInitializer.class,
	DeletedTeacherCardMapper.class,
    DateTimeMapper.class
})
class TeacherServiceTest {

	static final AcademicRank RANK_LECTURER = AcademicRank.LECTURER;
    static final AcademicRank RANK_PROFESSOR = AcademicRank.PROFESSOR;

	static final String VALID_PASSWORD = "Abcd1234!";
	static final String FIRST_NAME = "Alice";
	static final String LAST_NAME = "Smith";

	static final String OFFICE_A = "B-101";
	static final String OFFICE_B = "C-202";
	static final String BAD_OFFICE = "BAD_OFFICE";

	static final String LOCK_COURSE_CODE = "SEC-731";
	static final String LOCK_COURSE_NAME = "Locked Course";
	
	static final String EMAIL_CREATE_HAPPY = "teacher.create.happy@example.com";
	static final String EMAIL_UPDATE_HAPPY = "teacher.update.happy@example.com";
	static final String EMAIL_NULL_OFFICE = "teacher.null.office@example.com";
	static final String EMAIL_BAD_OFFICE = "teacher.bad.office@example.com";
	static final String EMAIL_NULL_RANK = "teacher.null.rank@example.com";
	static final String EMAIL_DELETE_BY_ID = "teacher.delete.by.id@example.com";
	static final String EMAIL_DELETED_CARD = "teacher.deleted.card@example.com";

	static final String DUP_EMAIL = "dup-teacher@example.com";
	static final String EMAIL_DELETE = "teacher.delete@example.com";
	static final String NOT_AN_EMAIL = "not-an-email";

	static final String EMAIL_FOR_UPDATES = "teacher.updates@example.com";
	static final String EMAIL_WITH_COURSE = "teacher.with.courses@example.com";

	static final Long NON_EXISTENT_ID = 999L;

	@Autowired TeacherService teacherService;
	@Autowired TestDataInitializer initializer;

	Long teacherForUpdatesId, teacherWithCoursesId;
	Teacher tempTeacher;

	TeacherCreateDto newTeacher(String email, String office, AcademicRank rank) {
		return new TeacherCreateDto(email, VALID_PASSWORD, FIRST_NAME, LAST_NAME, rank, office);
	}

	TeacherSelfUpdateDto patch(Long id, AcademicRank rank, String office) {
		return new TeacherSelfUpdateDto(id, rank, office);
	}

	@BeforeAll
	void setup() {
		var tUpdates = teacherService.createAll(List.of(newTeacher(EMAIL_FOR_UPDATES, OFFICE_A, RANK_LECTURER))).getFirst();
		teacherForUpdatesId = tUpdates.getId();

		var tWithCourse = teacherService.createAll(List.of(newTeacher(EMAIL_WITH_COURSE, OFFICE_A, RANK_LECTURER))).getFirst();
		teacherWithCoursesId = tWithCourse.getId();

		initializer.persistAll(Course.builder()
				.code(LOCK_COURSE_CODE)
				.name(LOCK_COURSE_NAME)
				.description(null)
				.teacher(tWithCourse)
				.build());
	}

	@AfterEach
	void cleanup() {
		Optional.ofNullable(tempTeacher).ifPresent(user -> teacherService.deleteById(tempTeacher.getId()));
		tempTeacher = null;
	}

	@Test
	@DisplayName("createAll: happy path — creates TEACHER with role TEACHER")
	void createAll_happyPath_success() {
        tempTeacher = teacherService.createAll(List.of(newTeacher(EMAIL_CREATE_HAPPY, OFFICE_A, RANK_LECTURER)))
                .getFirst();

	    assertNotNull(tempTeacher.getId());
	    assertNotNull(tempTeacher.getUser());
	    assertEquals(EMAIL_CREATE_HAPPY, tempTeacher.getUser().getEmail());
	    assertEquals(OFFICE_A, tempTeacher.getOffice());
	    assertEquals(RANK_LECTURER, tempTeacher.getAcademicRank());
	    assertEquals(UserRole.TEACHER, tempTeacher.getUser().getRole());
	}

	@Test
	@DisplayName("createAll: two teachers with the same email in one batch -> IllegalArgumentException")
	void createAll_twoTeachersSameEmail_oneBatch_fails() {
		var t1 = newTeacher(DUP_EMAIL, OFFICE_A, RANK_LECTURER);
		var t2 = newTeacher(DUP_EMAIL, OFFICE_B, RANK_LECTURER);
		assertThrows(IllegalArgumentException.class, () -> teacherService.createAll(List.of(t1, t2)));
	}

	@Test
	@DisplayName("createAll: duplicate email across two calls -> IllegalArgumentException")
	void createAll_duplicateEmail_acrossTwoCalls_fails() {
        tempTeacher = teacherService.createAll(List.of(newTeacher(DUP_EMAIL, OFFICE_A, RANK_LECTURER))).getFirst();
		assertThrows(IllegalArgumentException.class,
				() -> teacherService.createAll(List.of(newTeacher(DUP_EMAIL, OFFICE_B, RANK_LECTURER))));
	}

	@Test
	@DisplayName("createAll: invalid email -> ConstraintViolationException")
	void createAll_invalidEmail_fails() {
		var bad = newTeacher(NOT_AN_EMAIL, OFFICE_A, RANK_LECTURER);
		assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null office -> ConstraintViolationException (DTO validation)")
	void createAll_nullOffice_fails() {
	    var bad = newTeacher(EMAIL_NULL_OFFICE, null, RANK_LECTURER);
	    assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: bad office pattern -> ConstraintViolationException")
	void createAll_badOfficePattern_fails() {
	    var bad = newTeacher(EMAIL_BAD_OFFICE, BAD_OFFICE, RANK_LECTURER);
	    assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null academicRank -> ConstraintViolationException")
	void createAll_nullRank_fails() {
	    var bad = new TeacherCreateDto(EMAIL_NULL_RANK, VALID_PASSWORD, FIRST_NAME, LAST_NAME, null, OFFICE_A);
	    assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("updateSelf: happy path — updates rank and office")
	void updateSelf_happyPath_success() {
        tempTeacher = teacherService.createAll(List.of(newTeacher(EMAIL_UPDATE_HAPPY, OFFICE_A, RANK_LECTURER)))
                .getFirst();

	    var updated = teacherService.updateSelf(patch(tempTeacher.getId(), RANK_PROFESSOR, OFFICE_B));

	    assertEquals(tempTeacher.getId(), updated.getId());
	    assertEquals(RANK_PROFESSOR, updated.getAcademicRank());
	    assertEquals(OFFICE_B, updated.getOffice());
	}

	@Test
	@DisplayName("updateSelf: invalid office format -> ConstraintViolationException")
	void updateSelf_invalidOffice_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> teacherService.updateSelf(patch(teacherForUpdatesId, null, BAD_OFFICE)));
	}

	@Test
	@DisplayName("updateSelf: office = empty string -> ConstraintViolationException")
	void updateSelf_emptyOffice_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> teacherService.updateSelf(patch(teacherForUpdatesId, null, "")));
	}

	@Test
	@DisplayName("updateSelf: only office provided -> rank remains unchanged")
	void updateSelf_onlyOffice_rankUnchanged() {
        tempTeacher = teacherService
                .createAll(List.of(newTeacher("upd-office-only@example.com", OFFICE_A, RANK_LECTURER)))
                .getFirst();
		
		var updated = teacherService.updateSelf(patch(tempTeacher.getId(), null, OFFICE_B));
		
		assertEquals(OFFICE_B, updated.getOffice());
		assertEquals(RANK_LECTURER, updated.getAcademicRank());
	}

	@Test
	@DisplayName("deleteByIds: teacher has courses -> IllegalStateException")
	void deleteByIds_teacherHasCourses_fails() {
		assertThrows(IllegalStateException.class,
				() -> teacherService.deleteById(teacherWithCoursesId),
				"Should fail when teacher has courses");
	}
	
    @Test
    @DisplayName("deleteById: soft deletes active teacher")
    void deleteById_activeTeacher_softDeletes() {
        var saved = teacherService.createAll(List.of(newTeacher(EMAIL_DELETE_BY_ID, OFFICE_A, RANK_LECTURER)))
                .getFirst();

        teacherService.deleteById(saved.getId());

        assertTrue(teacherService.findByIds(List.of(saved.getId())).isEmpty());
        assertThrows(EntityNotFoundException.class, () -> teacherService.getTeacherProfileView(saved.getId()));
    }
    
    @Test
    @DisplayName("deleteById: teacher has courses -> IllegalStateException")
    void deleteById_teacherHasCourses_fails() {
        assertThrows(IllegalStateException.class, () -> teacherService.deleteById(teacherWithCoursesId),
                "Should fail when teacher has assigned courses");
    }
    
    @Test
    @DisplayName("deleteById: missing or already deleted teacher -> EntityNotFoundException")
    void deleteById_missingOrDeletedTeacher_fails() {
        assertThrows(EntityNotFoundException.class, () -> teacherService.deleteById(NON_EXISTENT_ID));
    }
    
    @Test
    @DisplayName("listDeletedTeacherCardsForAdmin: returns soft-deleted teachers with deletedAt")
    void listDeletedTeacherCardsForAdmin_returnsDeletedTeachers() {
        var saved = teacherService.createAll(List.of(newTeacher(EMAIL_DELETED_CARD, OFFICE_A, RANK_LECTURER)))
                .getFirst();

        teacherService.deleteById(saved.getId());

        var page = teacherService.listDeletedTeacherCardsForAdmin(PageRequest.of(0, 10));
        var deleted = page.getContent()
                .stream()
                .filter(card -> card.id().equals(saved.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(EMAIL_DELETED_CARD, deleted.email());
        assertEquals(FIRST_NAME, deleted.firstName());
        assertEquals(LAST_NAME, deleted.lastName());
        assertEquals(RANK_LECTURER, deleted.academicRank());
        assertEquals(OFFICE_A, deleted.office());
        assertNotNull(deleted.createdAt());
        assertNotNull(deleted.deletedAt());
    }

	@Test
	@DisplayName("getTeacherProfileView: happy path — returns TeacherProfileView for existing teacher")
	void getTeacherProfileView_happyPath_success() {
        tempTeacher = teacherService
                .createAll(List.of(newTeacher("profile.teacher@example.com", OFFICE_A, RANK_LECTURER)))
                .getFirst();

		var view = teacherService.getTeacherProfileView(tempTeacher.getId());

		assertNotNull(view);
		assertEquals(tempTeacher.getUser().getEmail(), view.email());
		assertEquals(tempTeacher.getUser().getFirstName(), view.firstName());
		assertEquals(tempTeacher.getUser().getLastName(), view.lastName());
		assertNotNull(view.createdAt(), "createdAt must be populated by DB");
		assertEquals(tempTeacher.getAcademicRank(), view.academicRank());
		assertEquals(tempTeacher.getOffice(), view.office());
	}

	@Test
	@DisplayName("getTeacherProfileView: missing id -> EntityNotFoundException")
	void getTeacherProfileView_missingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> teacherService.getTeacherProfileView(NON_EXISTENT_ID));
	}
}
