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
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.service.dto.DeleteResult;
import ua.foxminded.university.service.exception.ScheduleConflictException;
import ua.foxminded.university.testutil.TestDataInitializer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({ TestcontainersConfiguration.class, ScheduleEntryService.class, ValidatorConfig.class, EntityValidatior.class,
		TestDataInitializer.class })

class ScheduleEntryServiceTest {

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
	private ScheduleEntryService scheduleService;
	@Autowired
	private TestDataInitializer initializer;

	private Teacher teacherAlgDs, teacherNet;
	private StudyGroup groupCS, groupSE, groupNoCourse;
	private Course courseALG, courseDS, courseNET;
	private ScheduleEntry base;

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

		base = scheduleService.create(draft(courseALG.getId(), groupCS.getId(), T10, T12, ROOM_A),
				teacherAlgDs.getId());
	}

	private ScheduleEntry draft(Long courseId, Long groupId, OffsetDateTime start, OffsetDateTime end, String room) {
		return ScheduleEntry.builder()
				.course(Course.builder().id(courseId).build())
				.group(StudyGroup.builder().id(groupId).build())
				.startTime(start)
				.endTime(end)
				.room(room)
				.lessonType(null)
				.description("  note  ")
				.build();
	}

	@Test
	@DisplayName("happy path: create -> updateSelf (course, group, time, room, desc, type) -> deleteByIds")
	void happyPath_fullCycle_success() {
		var created = assertDoesNotThrow(() -> scheduleService
				.create(draft(courseALG.getId(), groupCS.getId(), T13, T14, ROOM_A), teacherAlgDs.getId()));

		assertNotNull(created.getId());
		assertEquals(ROOM_A, created.getRoom());
		assertEquals(LessonType.OTHER, created.getLessonType());
		assertEquals("note", created.getDescription());

		var patch1 = ScheduleEntry.builder()
				.id(created.getId())
				.group(StudyGroup.builder().id(groupSE.getId()).build())
				.build();
		var afterGroup = assertDoesNotThrow(() -> scheduleService.updateSelf(patch1, teacherAlgDs.getId()));
		assertEquals(groupSE.getId(), afterGroup.getGroup().getId());

		var patch2 = ScheduleEntry.builder()
				.id(created.getId())
				.course(Course.builder().id(courseDS.getId()).build())
				.build();
		var afterCourse = assertDoesNotThrow(() -> scheduleService.updateSelf(patch2, teacherAlgDs.getId()));
		assertEquals(courseDS.getId(), afterCourse.getCourse().getId());

		var patch3 = ScheduleEntry.builder().id(created.getId()).startTime(T14).endTime(T15).build();
		var afterTime = assertDoesNotThrow(() -> scheduleService.updateSelf(patch3, teacherAlgDs.getId()));
		assertEquals(T14, afterTime.getStartTime());
		assertEquals(T15, afterTime.getEndTime());

		var patch4 = ScheduleEntry.builder().id(created.getId()).room(" " + ROOM_B.toLowerCase() + " ").build();
		var afterRoom = assertDoesNotThrow(() -> scheduleService.updateSelf(patch4, teacherAlgDs.getId()));
		assertEquals(ROOM_B, afterRoom.getRoom());

		var patch5 = ScheduleEntry.builder().id(created.getId()).lessonType(LessonType.LAB).description("   ").build();
		var afterTypeDesc = assertDoesNotThrow(() -> scheduleService.updateSelf(patch5, teacherAlgDs.getId()));
		assertEquals(LessonType.LAB, afterTypeDesc.getLessonType());
		assertNull(afterTypeDesc.getDescription());

		DeleteResult res = assertDoesNotThrow(
				() -> scheduleService.deleteByIds(List.of(created.getId(), MISSING_ID), teacherAlgDs.getId()));
		assertEquals(List.of(created.getId()), res.deletedIds());
		assertEquals(List.of(MISSING_ID), res.notFoundIds());
	}

	@Test
	@DisplayName("create: teacherId == null -> IllegalArgumentException")
	void create_nullTeacherId_fails() {
		assertThrows(IllegalArgumentException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupCS.getId(), T10, T12, ROOM_A), null));
	}

	@Test
	@DisplayName("create: null course -> ConstraintViolationException (bean validation)")
	void create_nullCourse_fails() {
		var badCourse = ScheduleEntry.builder()
				.group(StudyGroup.builder().id(groupCS.getId()).build())
				.startTime(T10)
				.endTime(T12)
				.room(ROOM_A)
				.build();

		assertThrows(ConstraintViolationException.class, () -> scheduleService.create(badCourse, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: null group -> ConstraintViolationException (bean validation)")
	void create_nullGroup_fails() {
		var badGroup = ScheduleEntry.builder()
				.course(Course.builder().id(courseALG.getId()).build())
				.startTime(T10)
				.endTime(T12)
				.room(ROOM_A)
				.build();

		assertThrows(ConstraintViolationException.class, () -> scheduleService.create(badGroup, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: the course does not belong to the teacher -> IllegalStateException")
	void create_courseNotOwnedByTeacher_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.create(draft(courseNET.getId(), groupCS.getId(), T10, T12, ROOM_A),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: the group is not assigned to the course -> IllegalStateException")
	void create_groupNotAttachedToCourse_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.create(draft(courseDS.getId(), groupCS.getId(), T10, T12, ROOM_A),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: room = teacher's office -> IllegalStateException")
	void create_roomIsTeacherOffice_fails() {
		assertThrows(IllegalStateException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupCS.getId(), T10, T12, TEACHER_ALG_DS_OFFICE),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: missing course -> EntityNotFoundException")
	void create_missingCourse_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService.create(draft(MISSING_ID, groupCS.getId(), T13, T14, ROOM_B),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: missing group -> EntityNotFoundException")
	void create_missingGroup_fails() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService.create(draft(courseALG.getId(), MISSING_ID, T13, T14, ROOM_B),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: time/resource conflict -> ScheduleConflictException")
	void create_overlap_conflict_group_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupCS.getId(), T11, T13, ROOM_B),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: time/resource conflict -> ScheduleConflictException")
	void create_overlap_conflict_room_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupSE.getId(), T11, T13, ROOM_A),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: teacher overlap only -> ScheduleConflictException")
	void create_overlap_conflict_teacher_fails() {
		assertThrows(ScheduleConflictException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupSE.getId(), T11, T13, ROOM_B),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("create: end <= start -> IllegalStateException/ConstraintViolationException")
	void create_badTimeRange_fails() {
		assertThrows(ConstraintViolationException.class,
				() -> scheduleService.create(draft(courseALG.getId(), groupCS.getId(), T12, T11, ROOM_A),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: no-op patches are safe")
	void updateSelf_noop_ok() {
		var patch = ScheduleEntry.builder()
				.id(base.getId())
				.course(Course.builder().id(base.getCourse().getId()).build())
				.group(StudyGroup.builder().id(base.getGroup().getId()).build())
				.room(" " + base.getRoom().toLowerCase() + " ")
				.build();

		var after = assertDoesNotThrow(() -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
		assertEquals(base.getCourse().getId(), after.getCourse().getId());
		assertEquals(base.getGroup().getId(), after.getGroup().getId());
		assertEquals(base.getRoom(), after.getRoom());
	}

	@Test
	@DisplayName("updateSelf: switch to missing course -> IllegalStateException (checked as 'not owned')")
	void updateSelf_missingCourseId_reportsNotOwned() {
		var patch = ScheduleEntry.builder().id(base.getId()).course(Course.builder().id(MISSING_ID).build()).build();
		assertThrows(IllegalStateException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: not found -> EntityNotFoundException")
	void updateSelf_invalidArgsOrNotFound() {
		assertThrows(EntityNotFoundException.class,
				() -> scheduleService.updateSelf(
						ScheduleEntry.builder().id(MISSING_ID).lessonType(LessonType.LAB).build(),
						teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: change of course to someone else's -> IllegalStateException")
	void updateSelf_switchToForeignCourse_fails() {
		var patch = ScheduleEntry.builder()
				.id(base.getId())
				.course(Course.builder().id(courseNET.getId()).build())
				.build();

		assertThrows(IllegalStateException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: the group is not attached to the current course -> IllegalStateException")
	void updateSelf_groupNotAttached_fails() {
		var patch = ScheduleEntry.builder()
				.id(base.getId())
				.group(StudyGroup.builder().id(groupNoCourse.getId()).build())
				.build();

		assertThrows(IllegalStateException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: end <= start -> IllegalStateException")
	void updateSelf_badTimeRange_fails() {
		var patch = ScheduleEntry.builder().id(base.getId()).startTime(T12).endTime(T11).build();

		assertThrows(IllegalStateException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: room = teacher's office -> IllegalStateException")
	void updateSelf_roomIsTeacherOffice_fails() {
		var patch = ScheduleEntry.builder().id(base.getId()).room(TEACHER_NET_OFFICE).build();
		assertThrows(IllegalStateException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: invalid room -> ConstraintViolationException")
	void updateSelf_badRoomPattern_fails() {
		var patch = ScheduleEntry.builder().id(base.getId()).room(BAD_ROOM).build();
		assertThrows(ConstraintViolationException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: null patch -> IllegalArgumentException")
	void updateSelf_nullPatch_fails() {
		assertThrows(IllegalArgumentException.class, () -> scheduleService.updateSelf(null, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: null id in patch -> IllegalArgumentException")
	void updateSelf_nullId_fails() {
		var patch = ScheduleEntry.builder().lessonType(LessonType.LECTURE).build();
		assertThrows(IllegalArgumentException.class, () -> scheduleService.updateSelf(patch, teacherAlgDs.getId()));
	}

	@Test
	@DisplayName("updateSelf: null teacherId -> IllegalArgumentException")
	void updateSelf_nullTeacherId_fails() {
		var patch = ScheduleEntry.builder().id(base.getId()).lessonType(LessonType.LECTURE).build();
		assertThrows(IllegalArgumentException.class, () -> scheduleService.updateSelf(patch, null));
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
