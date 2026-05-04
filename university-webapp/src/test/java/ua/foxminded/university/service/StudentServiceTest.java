package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.security.config.PasswordEncoderConfig;
import ua.foxminded.university.service.dto.request.student.StudentCreateDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.projectionmapper.DateTimeMapper;
import ua.foxminded.university.service.util.projectionmapper.DeletedStudentCardMapper;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ 
    TestcontainersConfiguration.class,
    StudentService.class,
    AppUserService.class,
    TestDataInitializer.class,
	ValidatorConfig.class,
	EntityValidatior.class,
	PasswordPolicy.class,
	PasswordEncoderConfig.class,
	DtoMapper.class,
	DuplicateGuard.class,
	DeletedStudentCardMapper.class,
	DateTimeMapper.class
})
class StudentServiceTest {

	static final String DEFAULT_GROUP_NAME = "CS-101";
	static final String UPDATED_GROUP_NAME = "CS-102";
	
	static final String EMAIL_CREATE_HAPPY = "student.create.happy@example.com";
	static final String EMAIL_DELETE_HAPPY = "student.delete.happy@example.com";
	static final String EMAIL_NON_EXISTING_GROUP = "student.bad.group@example.com";
	static final String EMAIL_MOVE_HAPPY = "student.move.happy@example.com";
	static final String EMAIL_NULL_GROUP = "student.null.group@example.com";
	static final String EMAIL_DELETE_BY_ID = "student.delete.by.id@example.com";
	static final String EMAIL_DELETED_CARD = "student.deleted.card@example.com";

	static final String DUPLICATED_EMAIL = "dup@example.com";
	static final String MOOVE_NON_GRUP_EMAIL = "move-nogroup-1@example.com";
	static final String NOT_AN_EMAIL = "not-an-email";

	static final String VALID_PASSWORD = "Abcd1234!";
	static final String VALID_FIRST_NAME = "John";
	static final String UPDATED_FIRST_NAME = "UpdatedJohn";
	static final String VALID_LAST_NAME = "O'Connor";
	static final String UPDATED_LAST_NAME = "UpdatedO'Connor";

	static final Integer VALID_ENROLLMENT_YEAR = 2024;
	static final Integer ONE_STUDENT_MOOVED = 1;

	static final Long MISSING_ID = 999L;

	@Autowired StudentService studentService;
	@Autowired TestDataInitializer initializer;
	
	StudyGroup defaultGroup, updatedGroup;
	Student temp;

	StudentCreateDto dto(String email, String password, String first, String last, Long groupId, Integer year) {
		return new StudentCreateDto(email, password, first, last, groupId, year);
	}

	@BeforeAll
	void setup() {
		defaultGroup = initializer.persistAll(StudyGroup.builder().name(DEFAULT_GROUP_NAME).build()).getFirst();
		updatedGroup = initializer.persistAll(StudyGroup.builder().name(UPDATED_GROUP_NAME).build()).getFirst();
	}

	@AfterEach
	void cleanup() {
		Optional.ofNullable(temp).ifPresent(user -> studentService.deleteById(temp.getId()));
		temp = null;
	}

	@Test
	@DisplayName("createAll: happy path — creates Student with STUDENT role and linked AppUser")
	void createAll_happyPath_success() {
	    var createDto = dto(
	            EMAIL_CREATE_HAPPY,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            defaultGroup.getId(),
	            VALID_ENROLLMENT_YEAR
	    );

	    temp = studentService.createAll(List.of(createDto)).getFirst();

		assertNotNull(temp.getId(), "Student ID should be assigned after persist");
		assertNotNull(temp.getUser(), "Student must reference an AppUser");
		assertEquals(temp.getUser().getId(), temp.getId(), "@MapsId requires Student.id to equal AppUser.id");
		assertEquals(EMAIL_CREATE_HAPPY, temp.getUser().getEmail(), "Email should be persisted as provided/normalized");
		assertEquals(defaultGroup.getId(), temp.getGroup().getId(), "Student should be linked to default group");
		assertEquals(UserRole.STUDENT, temp.getUser().getRole(), "Service must set role=STUDENT");
	}
	
	@Test
	@DisplayName("deleteById: soft deletes active student")
	void deleteById_activeStudent_softDeletes() {
	    var createDto = dto(
	            EMAIL_DELETE_BY_ID,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            defaultGroup.getId(),
	            VALID_ENROLLMENT_YEAR
	    );

        var saved = studentService.createAll(List.of(createDto)).getFirst();

        studentService.deleteById(saved.getId());

        assertTrue(studentService.findByIds(List.of(saved.getId())).isEmpty());
        assertThrows(EntityNotFoundException.class, () -> studentService.getStudentProfileView(saved.getId()));
	}
	
	@Test
	@DisplayName("listDeletedStudentCardsForAdmin: returns soft-deleted students with deletedAt")
	void listDeletedStudentCardsForAdmin_returnsDeletedStudents() {
	    var createDto = dto(
	            EMAIL_DELETED_CARD,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            defaultGroup.getId(),
	            VALID_ENROLLMENT_YEAR
	    );

	    var saved = studentService.createAll(List.of(createDto)).getFirst();

	    studentService.deleteById(saved.getId());

	    var page = studentService.listDeletedStudentCardsForAdmin(PageRequest.of(0, 10));
	    var deleted = page.getContent()
	            .stream()
	            .filter(card -> card.id().equals(saved.getId()))
	            .findFirst()
	            .orElseThrow();

	    assertEquals(EMAIL_DELETED_CARD, deleted.email());
	    assertEquals(VALID_FIRST_NAME, deleted.firstName());
	    assertEquals(VALID_LAST_NAME, deleted.lastName());
	    assertEquals(VALID_ENROLLMENT_YEAR, deleted.enrollmentYear());
	    assertEquals(defaultGroup.getName(), deleted.groupName());
	    assertNotNull(deleted.createdAt());
	    assertNotNull(deleted.deletedAt());
	}
	
	@Test
	@DisplayName("deleteById: missing or already deleted student -> EntityNotFoundException")
	void deleteById_missingOrDeletedStudent_fails() {
	    assertThrows(EntityNotFoundException.class,
	            () -> studentService.deleteById(MISSING_ID));
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

		temp = studentService.createAll(List.of(first)).getFirst();

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
	    var d = dto(
	            EMAIL_NON_EXISTING_GROUP,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            MISSING_ID,
	            VALID_ENROLLMENT_YEAR
	    );

	    assertThrows(EntityNotFoundException.class, () -> studentService.createAll(List.of(d)));
	}

	@Test
	@DisplayName("moveStudentsToGroup: happy path — moves existing student to target group")
	void moveStudentsToGroup_happyPath_success() {
	    var createDto = dto(
	            EMAIL_MOVE_HAPPY,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            defaultGroup.getId(),
	            VALID_ENROLLMENT_YEAR
	    );

	    temp = studentService.createAll(List.of(createDto)).getFirst();

		int moved = studentService.moveStudentsToGroup(List.of(temp.getId()), updatedGroup.getId());
		assertEquals(ONE_STUDENT_MOOVED, moved, "Exactly one student should be moved");

		var reloaded = studentService.findByIds(List.of(temp.getId())).getFirst();
		assertEquals(updatedGroup.getId(), reloaded.getGroup().getId(), "Student must be linked to updated group");
	}

	@Test
	@DisplayName("moveStudentsToGroup: contains non-existing student ids -> EntityNotFoundException")
	void moveStudentsToGroup_missingStudents_fails() {
		var ids = List.of(MISSING_ID);

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

		temp = studentService.createAll(List.of(d)).getFirst();

		assertThrows(EntityNotFoundException.class,
				() -> studentService.moveStudentsToGroup(List.of(temp.getId()), MISSING_ID),
				"Should fail when target StudyGroup does not exist");
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
	    var d = dto(
	            EMAIL_NULL_GROUP,
	            VALID_PASSWORD,
	            VALID_FIRST_NAME,
	            VALID_LAST_NAME,
	            null,
	            VALID_ENROLLMENT_YEAR
	    );

	    assertThrows(ConstraintViolationException.class, () -> studentService.createAll(List.of(d)));
	}

	@Test
	@DisplayName("getStudentProfileView: happy path — returns StudentProfileView with group name")
	void getStudentProfileView_happyPath_success() {
		var createDto = dto("profile.student@example.com",
				VALID_PASSWORD,
				VALID_FIRST_NAME,
				VALID_LAST_NAME,
				defaultGroup.getId(),
				VALID_ENROLLMENT_YEAR);

		temp = studentService.createAll(List.of(createDto)).getFirst();

		var view = studentService.getStudentProfileView(temp.getId());

		assertNotNull(view);
		assertEquals(temp.getUser().getEmail(), view.email());
		assertEquals(temp.getUser().getFirstName(), view.firstName());
		assertEquals(temp.getUser().getLastName(), view.lastName());
		assertNotNull(view.createdAt(), "createdAt must be populated by DB");
		assertEquals(temp.getEnrollmentYear(), view.enrollmentYear());
		assertEquals(defaultGroup.getName(), view.groupName());
	}

	@Test
	@DisplayName("getStudentProfileView: missing id -> EntityNotFoundException")
	void getStudentProfileView_missingId_fails() {
		assertThrows(EntityNotFoundException.class, () -> studentService.getStudentProfileView(MISSING_ID));
	}
}
