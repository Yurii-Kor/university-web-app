package ua.foxminded.university.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import ua.foxminded.university.model.domain.*;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.dto.request.lesson.LessonCreateDto;
import ua.foxminded.university.service.dto.request.lesson.LessonSelfUpdateDto;
import ua.foxminded.university.service.exception.ScheduleConflictException;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, LessonService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class, DtoMapper.class })

class LessonServiceTest {

	private static final OffsetDateTime T10 = OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime T11 = T10.plusHours(1);
	private static final OffsetDateTime T12 = T10.plusHours(2);
	private static final OffsetDateTime T13 = T10.plusHours(3);
	private static final OffsetDateTime T14 = T10.plusHours(4);
	private static final OffsetDateTime T15 = T10.plusHours(5);

	private static final String ROOM_A = "A-105";
	private static final String ROOM_B = "B-201";
	private static final String BAD_ROOM = "bad";
	private static final String TEACHER_ALG_DS_OFFICE = "A-101";
	private static final String TEACHER_NET_OFFICE = "B-999";

	private static final String CODE_ALG = "ALG-101";
	private static final String NAME_ALG = "Algorithms";
	private static final String CODE_DS = "DS-201";
	private static final String NAME_DS = "Data Structures";
	private static final String CODE_NET = "NET-301";
	private static final String NAME_NET = "Networks";

	private static final Long MISSING_ID = 999_999L;
	private static final Integer ONE_SCHEDULE_ENTRY = 1;

	@Autowired
	private LessonService scheduleService;
	@Autowired
	private TestDataInitializer initializer;

	private Teacher teacherAlgDs, teacherNet;
	private StudyGroup groupCS, groupSE, groupNoCourse;
	private Course courseALG, courseDS, courseNET;
	private Lesson base;

	private LessonCreateDto createDto(Long teacherId, Long courseId, Long groupId, OffsetDateTime start,
			OffsetDateTime end, String room) {

		return new LessonCreateDto(teacherId, courseId, groupId, start, end, room, null, "note");
	}

	private LessonCreateDto createDto(Long teacherId, Long courseId, Long groupId, OffsetDateTime start,
			OffsetDateTime end, String room, String description) {

		return new LessonCreateDto(teacherId, courseId, groupId, start, end, room, null, description);
	}

	private LessonSelfUpdateDto patchDto(Long id, Long teacherId, Long courseId, Long groupId, OffsetDateTime start,
			OffsetDateTime end, String room, LessonType type, String description) {

		return new LessonSelfUpdateDto(id, teacherId, courseId, groupId, start, end, room, type, description);
	}

	@BeforeAll
	void setup() {
		teacherAlgDs = initializer.persistAll(Teacher.builder()
				.user(AppUser.builder()
						.email("t1@example.com")
						.password("Abcd1234!")
						.role(UserRole.TEACHER)
						.firstName("Ada")
						.lastName("Lovelace")
						.enabled(true)
						.build())
				.academicRank(AcademicRank.PROFESSOR)
				.office(TEACHER_ALG_DS_OFFICE)
				.build()).get(0);

		teacherNet = initializer.persistAll(Teacher.builder()
				.user(AppUser.builder()
						.email("t2@example.com")
						.password("Abcd1234!")
						.role(UserRole.TEACHER)
						.firstName("Grace")
						.lastName("Hopper")
						.enabled(true)
						.build())
				.academicRank(AcademicRank.SENIOR_LECTURER)
				.office(TEACHER_NET_OFFICE)
				.build()).get(0);

		groupCS = initializer.persistAll(StudyGroup.builder().name("CS-101").build()).get(0);
		groupSE = initializer.persistAll(StudyGroup.builder().name("SE-102").build()).get(0);
		groupNoCourse = initializer.persistAll(StudyGroup.builder().name("NO-404").build()).get(0);

		courseALG = initializer.persistAll(Course.builder()
				.code(CODE_ALG)
				.name(NAME_ALG)
				.teacher(teacherAlgDs)
				.groups(Set.of(groupCS, groupSE))
				.build()).get(0);

		courseDS = initializer.persistAll(
				Course.builder().code(CODE_DS).name(NAME_DS).teacher(teacherAlgDs).groups(Set.of(groupSE)).build())
				.get(0);

		courseNET = initializer.persistAll(
				Course.builder().code(CODE_NET).name(NAME_NET).teacher(teacherNet).groups(Set.of(groupCS)).build())
				.get(0);

		base = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T10, T12, ROOM_A, "base"));
	}

	@Test
	@DisplayName("create: happy path — creates lesson with default type and description")
	void create_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A));

		assertNotNull(created.getId());
		assertEquals(teacherAlgDs.getId(), created.getCourse().getTeacher().getId());
		assertEquals(courseALG.getId(), created.getCourse().getId());
		assertEquals(groupCS.getId(), created.getGroup().getId());
		assertEquals(ROOM_A, created.getRoom());
		assertEquals(T13, created.getStartTime());
		assertEquals(T14, created.getEndTime());
		assertEquals(LessonType.OTHER, created.getLessonType());
		assertEquals("note", created.getDescription());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("create: teacherId == null -> ConstraintViolationException")
	void create_nullTeacherId_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.create(createDto(null, courseALG.getId(), groupCS.getId(), T10, T12, ROOM_A)));
	}

	@Test
	@DisplayName("create: null course -> ConstraintViolationException (bean validation)")
	void create_nullCourse_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.create(createDto(teacherAlgDs.getId(), null, groupCS.getId(), T10, T12, ROOM_A)));
	}

	@Test
	@DisplayName("create: null group -> ConstraintViolationException (bean validation)")
	void create_nullGroup_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), null, T10, T12, ROOM_A)));
	}

	@Test
	@DisplayName("create: the course does not belong to the teacher -> IllegalStateException")
	void create_courseNotOwnedByTeacher_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseNET.getId(), groupCS.getId(), T10, T12, ROOM_A)));
	}

	@Test
	@DisplayName("create: the group is not assigned to the course -> IllegalStateException")
	void create_groupNotAttachedToCourse_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseDS.getId(), groupCS.getId(), T10, T12, ROOM_A)));
	}

	@Test
	@DisplayName("create: room = teacher's office -> IllegalStateException")
	void create_roomIsTeacherOffice_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.create(createDto(teacherAlgDs
						.getId(), courseALG.getId(), groupCS.getId(), T10, T12, TEACHER_ALG_DS_OFFICE)));
	}

	@Test
	@DisplayName("create: missing course -> EntityNotFoundException")
	void create_missingCourse_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), MISSING_ID, groupCS.getId(), T13, T14, ROOM_B)));
	}

	@Test
	@DisplayName("create: missing group -> EntityNotFoundException")
	void create_missingGroup_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), MISSING_ID, T13, T14, ROOM_B)));
	}

	@Test
	@DisplayName("create: time/resource conflict (group) -> ScheduleConflictException")
	void create_overlap_conflict_group_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T11, T13, ROOM_B)));
	}

	@Test
	@DisplayName("create: time/resource conflict (room) -> ScheduleConflictException")
	void create_overlap_conflict_room_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupSE.getId(), T11, T13, ROOM_A)));
	}

	@Test
	@DisplayName("create: teacher overlap only -> ScheduleConflictException")
	void create_overlap_conflict_teacher_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupSE.getId(), T11, T13, ROOM_B)));
	}

	@Test
	@DisplayName("create: end <= start -> IllegalStateException")
	void create_badTimeRange_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService
						.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T12, T11, ROOM_A)));
	}

	@Test
	@DisplayName("updateSelf: happy path — change group")
	void updateSelf_changeGroup_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A));

		var afterGroup = scheduleService.updateSelf(
				patchDto(created.getId(), teacherAlgDs.getId(), null, groupSE.getId(), null, null, null, null, null));

		assertEquals(groupSE.getId(), afterGroup.getGroup().getId());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("updateSelf: happy path — change course (with same teacher and compatible group)")
	void updateSelf_changeCourse_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupSE.getId(), T13, T14, ROOM_A));

		var afterCourse = scheduleService.updateSelf(
				patchDto(created.getId(), teacherAlgDs.getId(), courseDS.getId(), null, null, null, null, null, null));

		assertEquals(courseDS.getId(), afterCourse.getCourse().getId());
		assertEquals(groupSE.getId(), afterCourse.getGroup().getId());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("updateSelf: happy path — change time range")
	void updateSelf_changeTime_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A));

		var afterTime = scheduleService
				.updateSelf(patchDto(created.getId(), teacherAlgDs.getId(), null, null, T14, T15, null, null, null));

		assertEquals(T14, afterTime.getStartTime());
		assertEquals(T15, afterTime.getEndTime());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("updateSelf: happy path — change room")
	void updateSelf_changeRoom_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A));

		var afterRoom = scheduleService.updateSelf(
				patchDto(created.getId(), teacherAlgDs.getId(), null, null, null, null, ROOM_B, null, null));

		assertEquals(ROOM_B, afterRoom.getRoom());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("updateSelf: happy path — change type and blank description -> null")
	void updateSelf_changeTypeAndDescription_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A, "desc"));

		var afterTypeDesc = scheduleService.updateSelf(
				patchDto(created.getId(), teacherAlgDs.getId(), null, null, null, null, null, LessonType.LAB, "   "));

		assertEquals(LessonType.LAB, afterTypeDesc.getLessonType());
		assertNull(afterTypeDesc.getDescription());

		scheduleService.deleteByIds(List.of(created.getId()), teacherAlgDs.getId());
	}

	@Test
	@DisplayName("updateSelf: no-op patches are safe")
	void updateSelf_noop_ok() {
		var after = scheduleService.updateSelf(patchDto(base.getId(),
				teacherAlgDs.getId(),
				base.getCourse().getId(),
				base.getGroup().getId(),
				null,
				null,
				base.getRoom(),
				null,
				null));

		assertEquals(base.getCourse().getId(), after.getCourse().getId());
		assertEquals(base.getGroup().getId(), after.getGroup().getId());
		assertEquals(base.getRoom(), after.getRoom());
	}

	@Test
	@DisplayName("updateSelf: switch to missing course -> IllegalStateException (checked as 'not owned')")
	void updateSelf_missingCourseId_reportsNotOwned() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.updateSelf(
						patchDto(base.getId(), teacherAlgDs.getId(), MISSING_ID, null, null, null, null, null, null)));
	}

	@Test
	@DisplayName("updateSelf: not found -> EntityNotFoundException")
	void updateSelf_notFound() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService.updateSelf(patchDto(MISSING_ID,
						teacherAlgDs.getId(),
						null,
						null,
						null,
						null,
						null,
						LessonType.LAB,
						null)));
	}

	@Test
	@DisplayName("updateSelf: change of course to someone else's -> IllegalStateException")
	void updateSelf_switchToForeignCourse_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.updateSelf(patchDto(base
						.getId(), teacherAlgDs.getId(), courseNET.getId(), null, null, null, null, null, null)));
	}

	@Test
	@DisplayName("updateSelf: the group is not attached to the current course -> IllegalStateException")
	void updateSelf_groupNotAttached_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.updateSelf(patchDto(base
						.getId(), teacherAlgDs.getId(), null, groupNoCourse.getId(), null, null, null, null, null)));
	}

	@Test
	@DisplayName("updateSelf: end <= start -> IllegalStateException")
	void updateSelf_badTimeRange_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.updateSelf(
						patchDto(base.getId(), teacherAlgDs.getId(), null, null, T12, T11, null, null, null)));
	}

	@Test
	@DisplayName("updateSelf: room = teacher's office -> IllegalStateException")
	void updateSelf_roomIsTeacherOffice_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.updateSelf(patchDto(base
						.getId(), teacherAlgDs.getId(), null, null, null, null, TEACHER_NET_OFFICE, null, null)));
	}

	@Test
	@DisplayName("updateSelf: invalid room -> ConstraintViolationException")
	void updateSelf_badRoomPattern_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.updateSelf(
						patchDto(base.getId(), teacherAlgDs.getId(), null, null, null, null, BAD_ROOM, null, null)));
	}

	@Test
	@DisplayName("updateSelf: null patch -> IllegalArgumentException")
	void updateSelf_nullPatch_fails() {
		assertThrows(IllegalArgumentException.class, () -> scheduleService.updateSelf(null));
	}

	@Test
	@DisplayName("updateSelf: null id in patch -> ConstraintViolationException")
	void updateSelf_nullId_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.updateSelf(
						patchDto(null, teacherAlgDs.getId(), null, null, null, null, null, LessonType.LECTURE, null)));
	}

	@Test
	@DisplayName("updateSelf: null teacherId -> ConstraintViolationException")
	void updateSelf_nullTeacherId_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.updateSelf(
						patchDto(base.getId(), null, null, null, null, null, null, LessonType.LECTURE, null)));
	}

	@Test
	@DisplayName("findByIds: null/empty/contains null -> correct response")
	void findByIds_various() {
		assertTrue(scheduleService.findByIds(null).isEmpty());
		assertTrue(scheduleService.findByIds(List.of()).isEmpty());

		var res = scheduleService.findByIds(Arrays.asList(null, base.getId(), base.getId()));
		assertEquals(ONE_SCHEDULE_ENTRY, res.size());
		assertEquals(base.getId(), res.getFirst().getId());
	}

	@Test
	@DisplayName("deleteByIds: happy path — delete existing lesson and report notFound")
	void deleteByIds_happyPath_success() {
		var created = scheduleService
				.create(createDto(teacherAlgDs.getId(), courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A));

		var res = scheduleService.deleteByIds(List.of(created.getId(), MISSING_ID), teacherAlgDs.getId());

		assertEquals(Set.of(created.getId()), res.deletedIds());
		assertEquals(Set.of(MISSING_ID), res.notFoundIds());
	}

	@Test
	@DisplayName("deleteByIds: null/empty -> empty result")
	void deleteByIds_nullOrEmpty_returnsEmpty() {
		var r1 = scheduleService.deleteByIds(null, teacherAlgDs.getId());
		var r2 = scheduleService.deleteByIds(List.of(), teacherAlgDs.getId());
		assertTrue(r1.deletedIds().isEmpty() && r1.notFoundIds().isEmpty());
		assertTrue(r2.deletedIds().isEmpty() && r2.notFoundIds().isEmpty());
	}

	@Test
	@DisplayName("deleteByIds: teacherId == null -> IllegalArgumentException")
	void deleteByIds_nullTeacher_fails() {
		assertThrows(IllegalArgumentException.class, () -> scheduleService.deleteByIds(List.of(base.getId()), null));
	}

	@Test
	@DisplayName("deleteByIds: attempt to delete other schedule entry -> IllegalStateException")
	void deleteByIds_notOwned_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.deleteByIds(List.of(base.getId()), teacherNet.getId()));
	}
}
