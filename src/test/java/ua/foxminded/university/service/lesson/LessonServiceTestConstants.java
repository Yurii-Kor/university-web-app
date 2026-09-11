package ua.foxminded.university.service.lesson;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.experimental.UtilityClass;
import ua.foxminded.university.model.domain.enums.LessonType;

@UtilityClass
class LessonServiceTestConstants {

	final long TEACHER_1_ID = 1101L;
	final long TEACHER_2_ID = 1102L;

	final long GROUP_1_ID = 2001L;
	final long GROUP_2_ID = 2002L;
	final long GROUP_3_ID = 2003L;

	final long COURSE_1_ID = 3001L;
	final long COURSE_2_ID = 3002L;
	final long COURSE_3_ID = 3003L;

	final long GROUP_CONFLICT_LESSON_ID = 4001L;
	final long TEACHER_CONFLICT_LESSON_ID = 4002L;
	final long ROOM_CONFLICT_LESSON_ID = 4003L;

	final long UPDATE_TARGET_ID = 4100L;
	final long STARTED_LESSON_ID = 4101L;
	final long ASSIGNED_TEACHER_2_LESSON_ID = 4102L;

	final long MISSING_ID = 999_999L;

	final int BASE_LESSON_COUNT = 6;

	final OffsetDateTime HAPPY_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					16,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime HAPPY_END =
			HAPPY_START.plusHours(1);

	final OffsetDateTime DEFAULTS_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					17,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime DEFAULTS_END =
			DEFAULTS_START.plusHours(1);

	final OffsetDateTime GROUP_CONFLICT_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					10,
					15,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime GROUP_CONFLICT_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					10,
					45,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime TEACHER_CONFLICT_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					12,
					15,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime TEACHER_CONFLICT_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					12,
					45,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime ROOM_CONFLICT_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					14,
					15,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime ROOM_CONFLICT_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					14,
					45,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime ADJACENT_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					11,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime ADJACENT_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					12,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime UPDATE_TARGET_START =
			OffsetDateTime.of(
					2030,
					1,
					11,
					16,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime UPDATE_TARGET_END =
			UPDATE_TARGET_START.plusHours(1);

	final OffsetDateTime UPDATED_START =
			OffsetDateTime.of(
					2030,
					1,
					11,
					18,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime UPDATED_END =
			UPDATED_START.plusHours(1);

	final OffsetDateTime ASSIGNED_TEACHER_2_START =
			OffsetDateTime.of(
					2030,
					1,
					11,
					20,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime ASSIGNED_TEACHER_2_END =
			ASSIGNED_TEACHER_2_START.plusHours(1);

	final OffsetDateTime ASSIGNED_TEACHER_2_OVERLAP_START =
			ASSIGNED_TEACHER_2_START.plusMinutes(15);

	final OffsetDateTime ASSIGNED_TEACHER_2_OVERLAP_END =
			ASSIGNED_TEACHER_2_END.minusMinutes(15);

	final OffsetDateTime ASSIGNED_TEACHER_RANGE_START =
			ASSIGNED_TEACHER_2_START.minusHours(1);

	final OffsetDateTime ASSIGNED_TEACHER_RANGE_END =
			ASSIGNED_TEACHER_2_END.plusHours(1);

	final OffsetDateTime SCHEDULE_DAY_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					0,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime SCHEDULE_DAY_END =
			SCHEDULE_DAY_START.plusDays(1);

	final OffsetDateTime GROUP_RANGE_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					8,
					30,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime GROUP_RANGE_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					10,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime TEACHER_RANGE_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					10,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime TEACHER_RANGE_END =
			OffsetDateTime.of(
					2030,
					1,
					10,
					14,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime EMPTY_RANGE_START =
			OffsetDateTime.of(
					2030,
					1,
					12,
					0,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime STARTED_LESSON_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					8,
					30,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime TEACHER_CONFLICT_LESSON_START =
			OffsetDateTime.of(
					2030,
					1,
					10,
					12,
					0,
					0,
					0,
					ZoneOffset.UTC);

	final OffsetDateTime EMPTY_RANGE_END =
			EMPTY_RANGE_START.plusDays(1);

	final String CREATE_ROOM = "C-404";
	final String ROOM_CONFLICT = "R-303";
	final String TEACHER_1_OFFICE = "A-101";

	final String CREATE_DESCRIPTION = "New lesson";

	final LessonType CREATE_TYPE = LessonType.LECTURE;

	final String UPDATE_TARGET_ROOM = "U-404";
	final String UPDATE_TARGET_DESCRIPTION = "Update target";
	final LessonType UPDATE_TARGET_TYPE = LessonType.SEMINAR;

	final String UPDATED_ROOM = "D-505";
	final String UPDATED_DESCRIPTION = "Updated lesson";
	final LessonType UPDATED_TYPE = LessonType.EXAM;
}