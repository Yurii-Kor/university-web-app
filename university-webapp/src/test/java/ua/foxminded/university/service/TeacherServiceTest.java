package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
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
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, TeacherService.class, AppUserService.class, ValidatorConfig.class,
		EntityValidatior.class, PasswordPolicy.class, PasswordEncoderConfig.class, DtoMapper.class,
		DuplicateGuard.class, TestDataInitializer.class })
class TeacherServiceTest {

	// ---- constants
	static final AcademicRank RANK_A = AcademicRank.values()[0];
	static final AcademicRank RANK_B = AcademicRank.values().length > 1 ? AcademicRank.values()[1]
			: AcademicRank.values()[0];

	static final String VALID_EMAIL = "teacher1@example.com";
	static final String DUP_EMAIL = "dup-teacher@example.com";
	static final String EMAIL_DELETE = "teacher.delete@example.com";
	static final String NOT_AN_EMAIL = "not-an-email";

	static final String EMAIL_FOR_UPDATES = "teacher.updates@example.com";
	static final String EMAIL_WITH_COURSE = "teacher.with.courses@example.com";

	static final String VALID_PASSWORD = "Abcd1234!";
	static final String FIRST_NAME = "Alice";
	static final String LAST_NAME = "Smith";

	static final String OFFICE_A = "B-101";
	static final String OFFICE_B = "C-202";
	static final String BAD_OFFICE = "BAD_OFFICE";

	static final String LOCK_COURSE_CODE = "SEC-731";
	static final String LOCK_COURSE_NAME = "Locked Course";

	static final Long NON_EXISTENT_ID = 999L;

	@Autowired
	private TeacherService teacherService;
	@Autowired
	private TestDataInitializer initializer;

	private Teacher tempTeacher;

	private Long teacherForUpdatesId;
	private Long teacherWithCoursesId;

	private TeacherCreateDto newTeacher(String email, String office, AcademicRank rank) {
		return new TeacherCreateDto(email, VALID_PASSWORD, FIRST_NAME, LAST_NAME, rank, office);
	}

	private TeacherSelfUpdateDto patch(Long id, AcademicRank rank, String office) {
		return new TeacherSelfUpdateDto(id, rank, office);
	}

	@BeforeAll
	void setup() {
		Teacher tUpdates = teacherService.createAll(List.of(newTeacher(EMAIL_FOR_UPDATES, OFFICE_A, RANK_A))).get(0);
		teacherForUpdatesId = tUpdates.getId();

		Teacher tWithCourse = teacherService.createAll(List.of(newTeacher(EMAIL_WITH_COURSE, OFFICE_A, RANK_A))).get(0);
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
		Optional.ofNullable(tempTeacher).ifPresent(user -> teacherService.deleteByIds(List.of(tempTeacher.getId())));
		tempTeacher = null;
	}

	@Test
	@DisplayName("createAll: happy path — creates TEACHER with role TEACHER")
	void createAll_happyPath_success() {
		tempTeacher = teacherService.createAll(List.of(newTeacher(VALID_EMAIL, OFFICE_A, RANK_A))).get(0);

		assertNotNull(tempTeacher.getId());
		assertNotNull(tempTeacher.getUser());
		assertEquals(VALID_EMAIL, tempTeacher.getUser().getEmail());
		assertEquals(OFFICE_A, tempTeacher.getOffice());
		assertEquals(RANK_A, tempTeacher.getAcademicRank());
		assertEquals(UserRole.TEACHER, tempTeacher.getUser().getRole());

		teacherService.deleteByIds(List.of(tempTeacher.getId()));
	}

	@Test
	@DisplayName("createAll: two teachers with the same email in one batch -> IllegalArgumentException")
	void createAll_twoTeachersSameEmail_oneBatch_fails() {
		var t1 = newTeacher(DUP_EMAIL, OFFICE_A, RANK_A);
		var t2 = newTeacher(DUP_EMAIL, OFFICE_B, RANK_A);
		assertThrows(IllegalArgumentException.class, () -> teacherService.createAll(List.of(t1, t2)));
	}

	@Test
	@DisplayName("createAll: duplicate email across two calls -> IllegalArgumentException")
	void createAll_duplicateEmail_acrossTwoCalls_fails() {
		tempTeacher = teacherService.createAll(List.of(newTeacher(DUP_EMAIL, OFFICE_A, RANK_A))).get(0);
		assertThrows(IllegalArgumentException.class,
				() -> teacherService.createAll(List.of(newTeacher(DUP_EMAIL, OFFICE_B, RANK_A))));
	}

	@Test
	@DisplayName("createAll: invalid email -> ConstraintViolationException")
	void createAll_invalidEmail_fails() {
		var bad = newTeacher(NOT_AN_EMAIL, OFFICE_A, RANK_A);
		assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null office -> ConstraintViolationException (DTO validation)")
	void createAll_nullOffice_fails() {
		var bad = newTeacher(VALID_EMAIL, null, RANK_A);
		assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: bad office pattern -> ConstraintViolationException")
	void createAll_badOfficePattern_fails() {
		var bad = newTeacher(VALID_EMAIL, BAD_OFFICE, RANK_A);
		assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("createAll: null academicRank -> ConstraintViolationException")
	void createAll_nullRank_fails() {
		var bad = new TeacherCreateDto(VALID_EMAIL, VALID_PASSWORD, FIRST_NAME, LAST_NAME, null, OFFICE_A);
		assertThrows(ConstraintViolationException.class, () -> teacherService.createAll(List.of(bad)));
	}

	@Test
	@DisplayName("updateSelf: happy path — updates rank and office")
	void updateSelf_happyPath_success() {
		tempTeacher = teacherService.createAll(List.of(newTeacher(VALID_EMAIL, OFFICE_A, RANK_A))).get(0);

		var updated = teacherService.updateSelf(patch(tempTeacher.getId(), RANK_B, OFFICE_B));

		assertEquals(tempTeacher.getId(), updated.getId());
		assertEquals(RANK_B, updated.getAcademicRank());
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
		tempTeacher = teacherService.createAll(List.of(newTeacher("upd-office-only@example.com", OFFICE_A, RANK_A)))
				.get(0);
		var updated = teacherService.updateSelf(patch(tempTeacher.getId(), null, OFFICE_B));
		assertEquals(OFFICE_B, updated.getOffice());
		assertEquals(RANK_A, updated.getAcademicRank());
	}

	@Test
	@DisplayName("deleteByIds: teacher has courses -> IllegalStateException")
	void deleteByIds_teacherHasCourses_fails() {
		assertThrows(IllegalStateException.class,
				() -> teacherService.deleteByIds(List.of(teacherWithCoursesId)),
				"Should fail when teacher has courses");
	}

	@Test
	@DisplayName("deleteByIds: mix of existing and missing -> returns deleted & notFound as expected")
	void deleteByIds_mixedIds_returnsSplit() {
		var saved = teacherService.createAll(List.of(newTeacher(EMAIL_DELETE, OFFICE_A, RANK_A))).get(0);

		var result = teacherService.deleteByIds(Arrays.asList(saved.getId(), NON_EXISTENT_ID, null));

		assertEquals(Set.of(saved.getId()), result.deletedIds(), "should delete existing id");
		assertEquals(Set.of(NON_EXISTENT_ID), result.notFoundIds(), "should report missing id");
	}
}
