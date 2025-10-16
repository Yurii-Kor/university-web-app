package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, StudentService.class, AppUserService.class, TestDataInitializer.class,
		ValidatorConfig.class, EntityValidatior.class, PasswordPolicy.class, PasswordEncoderConfig.class })
class StudentServiceTest {

	static final String DEFAULT_GROUP_NAME = "CS-101";
	static final String UPDATED_GROUP_NAME = "CS-102";

	static final String VALID_EMAIL = "student1@example.com";
	static final String UPDATED_EMAIL = "student1updated@example.com";
	static final String DUPLICATED_EMAIL = "dup@example.com";
	static final String DUP_UPDATE_EMAIL_A = "dup-update-a@example.com";
	static final String DUP_UPDATE_EMAIL_B = "dup-update-b@example.com";
	static final String MOOVE_NON_GRUP_EMAIL = "move-nogroup-1@example.com";

	static final String VALID_PASSWORD = "Abcd1234!";
	static final String VALID_FIRST_NAME = "John";
	static final String UPDATED_FIRST_NAME = "UpdatedJohn";
	static final String VALID_LAST_NAME = "O'Connor";
	static final String UPDATED_LAST_NAME = "UpdatedO'Connor";

	static final Integer VALID_ENROLLMENT_YEAR = 2024;
	static final Integer ONE_STUDENT_MOOVED = 1;

	static final Long NON_EXISTENT_ID = 999L;

	@Autowired
	private StudentService studentService;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private TestDataInitializer initializer;

	private StudyGroup defaultGroup;
	private StudyGroup updatedGroup;

	@BeforeAll
	void setup() {
		defaultGroup = initializer.persistAll(StudyGroup.builder().name(DEFAULT_GROUP_NAME).build()).get(0);
		updatedGroup = initializer.persistAll(StudyGroup.builder().name(UPDATED_GROUP_NAME).build()).get(0);
	}

	@Test
	@DisplayName("StudentService happy path: create -> update self (email/name) -> delete")
	void create_updateSelf_delete_success() {
		var user = AppUser.builder()
				.email(VALID_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var toSave = Student.builder().user(user).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		var saved = assertDoesNotThrow(() -> studentService.createAll(List.of(toSave)).get(0),
				"createAll(List.of(student)) should not throw");

		assertNotNull(saved.getId(), "Student ID should be assigned after persist");
		assertNotNull(saved.getUser(), "Student must reference an AppUser");
		assertEquals(saved.getUser().getId(), saved.getId(), "@MapsId requires Student.id to equal AppUser.id");
		assertEquals(VALID_EMAIL,
				saved.getUser().getEmail(),
				"Email should be persisted as provided (normalized in entity if applicable)");
		assertEquals(saved.getGroup().getId(),
				defaultGroup.getId(),
				"Student should be linked to the seeded default group");

		var moved = assertDoesNotThrow(
				() -> studentService.moveStudentsToGroup(List.of(saved.getId()), updatedGroup.getId()),
				"moveStudentsToGroup should not throw for valid patch");

		assertEquals(moved, ONE_STUDENT_MOOVED, "Processed Student shold be mooved");

		var result = assertDoesNotThrow(() -> studentService.deleteByIds(List.of(saved.getId())),
				"deleteByIds should not throw for existing id");

		assertEquals(List.of(saved.getId()),
				result.deletedIds(),
				"Deleted IDs should contain exactly the student's id");
		assertTrue(result.notFoundIds().isEmpty(), "notFound should be empty for existing id");

		var afterDelete = studentService.findByIds(List.of(saved.getId()));
		assertTrue(afterDelete.isEmpty(), "Student must not be found after deletion");
	}

	@Test
	@DisplayName("createAll: two students in one call with the same email → DataIntegrityViolationException")
	void createAll_twoStudentsSameEmail_oneBatch_fails() {
		var u1 = AppUser.builder()
				.email(DUPLICATED_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var u2 = AppUser.builder()
				.email(DUPLICATED_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var s1 = Student.builder().user(u1).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();
		var s2 = Student.builder().user(u2).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		assertThrows(DataIntegrityViolationException.class,
				() -> studentService.createAll(List.of(s1, s2)),
				"Batch with duplicate user.email must fail on unique constraint");
	}

	@Test
	@DisplayName("createAll: second call with duplicate email (existing in DB) → DataIntegrityViolationException")
	void createAll_duplicateEmail_acrossTwoCalls_fails() {
		var firstUser = AppUser.builder()
				.email(DUPLICATED_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var first = Student.builder().user(firstUser).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();
		assertDoesNotThrow(() -> studentService.createAll(List.of(first)),
				"First insert with unique email should pass");

		var dupUser = AppUser.builder()
				.email(DUPLICATED_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var second = Student.builder().user(dupUser).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		assertThrows(DataIntegrityViolationException.class,
				() -> studentService.createAll(List.of(second)),
				"Second insert with duplicate user.email must fail on unique constraint");
	}

	@Test
	@DisplayName("createAll: non-existing StudyGroup (FK) → EntityNotFoundException")
	void createAll_nonExistingGroup_fails() {
		var user = AppUser.builder()
				.email(VALID_EMAIL)
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var nonExistingGroup = StudyGroup.builder().id(NON_EXISTENT_ID).build();

		var s = Student.builder().user(user).group(nonExistingGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		assertThrows(EntityNotFoundException.class,
				() -> studentService.createAll(List.of(s)),
				"Insert with non-existing group_id must fail on FK constraint");
	}

	@Test
	@DisplayName("moveStudentsToGroup: contains non-existing student ids -> EntityNotFoundException")
	void moveStudentsToGroup_missingStudents_fails() {
		var ids = List.of(NON_EXISTENT_ID);

		assertThrows(EntityNotFoundException.class,
				() -> studentService.moveStudentsToGroup(ids, updatedGroup.getId()),
				"Should fail when at least one student id does not exist");
	}

	@Test
	@DisplayName("moveStudentsToGroup: target group does not exist -> EntityNotFoundException")
	void moveStudentsToGroup_nonExistingGroup_fails() {
		var s = Student.builder()
				.user(AppUser.builder()
						.email(MOOVE_NON_GRUP_EMAIL)
						.password(VALID_PASSWORD)
						.role(UserRole.STUDENT)
						.firstName(VALID_FIRST_NAME)
						.lastName(VALID_LAST_NAME)
						.enabled(true)
						.build())
				.group(defaultGroup)
				.enrollmentYear(VALID_ENROLLMENT_YEAR)
				.build();

		var saved = studentService.createAll(List.of(s)).get(0);

		assertThrows(EntityNotFoundException.class,
				() -> studentService.moveStudentsToGroup(List.of(saved.getId()), NON_EXISTENT_ID),
				"Should fail when target StudyGroup does not exist");
	}

	@Test
	@DisplayName("moveStudentsToGroup: null targetGroupId -> IllegalArgumentException")
	void moveStudentsToGroup_nullTargetGroup_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> studentService.moveStudentsToGroup(List.of(1L, 2L), null),
				"Should fail when targetGroupId is null");
	}

	@Test
	@DisplayName("moveStudentsToGroup: null or empty student ids -> returns 0")
	void moveStudentsToGroup_nullOrEmptyIds_returnsZero() {
		assertEquals(0, studentService.moveStudentsToGroup(null, updatedGroup.getId()), "null ids must return 0");
		assertEquals(0, studentService.moveStudentsToGroup(List.of(), updatedGroup.getId()), "empty ids must return 0");
		assertEquals(0,
				studentService.moveStudentsToGroup(Arrays.asList(null, null), updatedGroup.getId()),
				"only-null ids must return 0");
	}

	@Test
	@DisplayName("createAll: user.role != STUDENT -> ConstraintViolationException")
	void createAll_invalidRole_fails() {
		var user = AppUser.builder()
				.email("badrole@example.com")
				.password(VALID_PASSWORD)
				.role(UserRole.TEACHER)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var s = Student.builder().user(user).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(s)),
				"Student.user.role must be STUDENT");
	}

	@Test
	@DisplayName("createAll: null user -> IllegalArgumentException")
	void createAll_nullUser_fails() {
		var s = Student.builder().user(null).group(defaultGroup).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(s)),
				"student.user must not be null");
	}

	@Test
	@DisplayName("createAll: null group or null group.id -> IllegalArgumentException")
	void createAll_nullGroupOrId_fails() {
		var u = AppUser.builder()
				.email("nullgrp@example.com")
				.password(VALID_PASSWORD)
				.role(UserRole.STUDENT)
				.firstName(VALID_FIRST_NAME)
				.lastName(VALID_LAST_NAME)
				.enabled(true)
				.build();

		var s1 = Student.builder().user(u).group(null).enrollmentYear(VALID_ENROLLMENT_YEAR).build();

		var s2 = Student.builder()
				.user(u.toBuilder().email("nullgrpid@example.com").build())
				.group(StudyGroup.builder().id(null).build())
				.enrollmentYear(VALID_ENROLLMENT_YEAR)
				.build();

		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(s1)),
				"student.group must not be null");
		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(s2)),
				"student.group.id must not be null");
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixedIds_returnsSplit() {
		var s = Student.builder()
				.user(AppUser.builder()
						.email("delmix@example.com")
						.password(VALID_PASSWORD)
						.role(UserRole.STUDENT)
						.firstName(VALID_FIRST_NAME)
						.lastName(VALID_LAST_NAME)
						.enabled(true)
						.build())
				.group(defaultGroup)
				.enrollmentYear(VALID_ENROLLMENT_YEAR)
				.build();

		var saved = studentService.createAll(List.of(s)).get(0);

		var result = studentService.deleteByIds(Arrays.asList(saved.getId(), NON_EXISTENT_ID, null));

		assertEquals(List.of(saved.getId()), result.deletedIds(), "should delete existing id");
		assertEquals(List.of(NON_EXISTENT_ID), result.notFoundIds(), "should report missing id");
	}
}
