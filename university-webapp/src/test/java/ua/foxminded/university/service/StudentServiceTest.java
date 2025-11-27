package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.service.dto.request.student.StudentCreateDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, StudentService.class, AppUserService.class, TestDataInitializer.class,
		ValidatorConfig.class, EntityValidatior.class, PasswordPolicy.class, PasswordEncoderConfig.class,
		DtoMapper.class, DuplicateGuard.class })
class StudentServiceTest {

	static final String DEFAULT_GROUP_NAME = "CS-101";
	static final String UPDATED_GROUP_NAME = "CS-102";

	static final String VALID_EMAIL = "student1@example.com";
	static final String UPDATED_EMAIL = "student1updated@example.com";
	static final String DUPLICATED_EMAIL = "dup@example.com";
	static final String DUP_UPDATE_EMAIL_A = "dup-update-a@example.com";
	static final String DUP_UPDATE_EMAIL_B = "dup-update-b@example.com";
	static final String MOOVE_NON_GRUP_EMAIL = "move-nogroup-1@example.com";
	static final String NOT_AN_EMAIL = "not-an-email";

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

	private StudentCreateDto dto(String email, String password, String first, String last, Long groupId, Integer year) {
		return new StudentCreateDto(email, password, first, last, groupId, year);
	}

	@BeforeAll
	void setup() {
		defaultGroup = initializer.persistAll(StudyGroup.builder().name(DEFAULT_GROUP_NAME).build()).get(0);
		updatedGroup = initializer.persistAll(StudyGroup.builder().name(UPDATED_GROUP_NAME).build()).get(0);
	}

	@Test
	@DisplayName("StudentService happy path: create(dto) -> move -> delete; роль выставляется как STUDENT")
	void happyPath_fullCycle_success() {
		var createDto = dto(VALID_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		var saved = assertDoesNotThrow(() -> studentService.createAll(List.of(createDto)).get(0),
				"createAll(List.of(dto)) should not throw");

		assertNotNull(saved.getId(), "Student ID should be assigned after persist");
		assertNotNull(saved.getUser(), "Student must reference an AppUser");
		assertEquals(saved.getUser().getId(), saved.getId(), "@MapsId requires Student.id to equal AppUser.id");
		assertEquals(VALID_EMAIL, saved.getUser().getEmail(), "Email should be persisted as provided/normalized");
		assertEquals(defaultGroup.getId(), saved.getGroup().getId(), "Student should be linked to default group");
		assertEquals(UserRole.STUDENT, saved.getUser().getRole(), "Service must set role=STUDENT");

		var moved = assertDoesNotThrow(
				() -> studentService.moveStudentsToGroup(List.of(saved.getId()), updatedGroup.getId()),
				"moveStudentsToGroup should not throw for valid patch");
		assertEquals(ONE_STUDENT_MOOVED, moved, "Processed Student should be moved");

		var result = assertDoesNotThrow(() -> studentService.deleteByIds(List.of(saved.getId())),
				"deleteByIds should not throw for existing id");

		assertEquals(Set.of(saved.getId()), result.deletedIds(), "Deleted IDs should contain the student's id");
		assertTrue(result.notFoundIds().isEmpty(), "notFound should be empty");

		var afterDelete = studentService.findByIds(List.of(saved.getId()));
		assertTrue(afterDelete.isEmpty(), "Student must not be found after deletion");
	}

	@Test
	@DisplayName("createAll: two students in one call with the same email → IllegalArgumentException")
	void createAll_twoStudentsSameEmail_oneBatch_fails() {
		var d1 = dto(DUPLICATED_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		var d2 = dto(DUPLICATED_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		assertThrows(IllegalArgumentException.class,
				() -> studentService.createAll(List.of(d1, d2)),
				"Batch with duplicate user.email must fail with IllegalArgumentException");
	}

	@Test
	@DisplayName("createAll: second call with duplicate email (existing in DB) → IllegalArgumentException")
	void createAll_duplicateEmail_acrossTwoCalls_fails() {
		var first = dto(DUPLICATED_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		assertDoesNotThrow(() -> studentService.createAll(List.of(first)),
				"First insert with unique email should pass");

		var second = dto(DUPLICATED_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		assertThrows(IllegalArgumentException.class,
				() -> studentService.createAll(List.of(second)),
				"Second insert with duplicate email must fail");
	}

	@Test
	@DisplayName("createAll: non-existing StudyGroup (FK) → EntityNotFoundException")
	void createAll_nonExistingGroup_fails() {
		var d = dto(VALID_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				NON_EXISTENT_ID,
				VALID_ENROLLMENT_YEAR);

		assertThrows(EntityNotFoundException.class,
				() -> studentService.createAll(List.of(d)),
				"Insert with non-existing group_id must fail");
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
		var d = dto(MOOVE_NON_GRUP_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		var saved = studentService.createAll(List.of(d)).get(0);

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
	@DisplayName("createAll: invalid DTO (bad email) -> ConstraintViolationException")
	void createAll_badEmail_fails() {
		var d = dto(NOT_AN_EMAIL,
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(d)),
				"bad email should trigger DTO validation");
	}

	@Test
	@DisplayName("createAll: invalid DTO (null groupId) -> ConstraintViolationException")
	void createAll_nullGroupId_fails() {
		var d = dto("nullgrp@example.com",
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				null,
				VALID_ENROLLMENT_YEAR);

		assertThrows(ConstraintViolationException.class,
				() -> studentService.createAll(List.of(d)),
				"groupId is @NotNull in DTO");
	}

	@Test
	@DisplayName("deleteByIds: mix existing/missing -> returns deleted & notFound as expected")
	void deleteByIds_mixedIds_returnsSplit() {
		var d = dto("delmix@example.com",
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		var saved = studentService.createAll(List.of(d)).get(0);

		var result = studentService.deleteByIds(Arrays.asList(saved.getId(), NON_EXISTENT_ID, null));

		assertEquals(Set.of(saved.getId()), result.deletedIds(), "should delete existing id");
		assertEquals(Set.of(NON_EXISTENT_ID), result.notFoundIds(), "should report missing id");
	}
}
