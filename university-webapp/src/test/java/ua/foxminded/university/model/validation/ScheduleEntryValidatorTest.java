package ua.foxminded.university.model.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.ScheduleEntry;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class ScheduleEntryValidatorTest {

	private static final String CASE_PATTERN = "group:{0}, course:{1}, start:{2}, end:{3}, room:\"{4}\", type:{5} -> valid={6}";

	private static final String NULL = "__NULL__";

	private static final StudyGroup VALID_GROUP = StudyGroup.builder().id(1L).name("CS-101").build();

	private static final Course VALID_COURSE = Course.builder()
			.id(10L)
			.code("SEC-303")
			.name("Application Security Fundamentals")
			.build();

	private static final LessonType VALID_TYPE = LessonType.LECTURE;

	private static final OffsetDateTime NOW = OffsetDateTime.now();
	private static final OffsetDateTime PLUS_1H = NOW.plusHours(1);
	private static final OffsetDateTime MINUS_1H = NOW.minusHours(1);

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_NO_DASH = "B105";
	private static final String ROOM_SHORT_DIG = "B-12";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("ScheduleEntry entity bean validation")
	void validateSchedule(StudyGroup group, Course course, OffsetDateTime start, OffsetDateTime end, String room,
			LessonType type, boolean shouldPass) {

		StudyGroup g = group == null ? null : group;
		Course c = course == null ? null : course;
		String r = NULL.equals(room) ? null : room;

		ScheduleEntry entry = ScheduleEntry.builder()
				.id(100L)
				.group(g)
				.course(c)
				.startTime(start)
				.endTime(end)
				.room(r)
				.lessonType(type)
				.build();

		Set<ConstraintViolation<ScheduleEntry>> violations = validator.validate(entry);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_VALID, VALID_TYPE, true),

				Arguments.of(null, VALID_COURSE, NOW, PLUS_1H, ROOM_VALID, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, null, NOW, PLUS_1H, ROOM_VALID, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, null, PLUS_1H, ROOM_VALID, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, null, ROOM_VALID, VALID_TYPE, false),

				Arguments.of(VALID_GROUP, VALID_COURSE, PLUS_1H, NOW, ROOM_VALID, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, NOW, ROOM_VALID, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, MINUS_1H, ROOM_VALID, VALID_TYPE, false),

				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, NULL, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_EMPTY, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_LOWER, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_NO_DASH, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_SHORT_DIG, VALID_TYPE, false),
				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_SPACED, VALID_TYPE, false),

				Arguments.of(VALID_GROUP, VALID_COURSE, NOW, PLUS_1H, ROOM_VALID, null, false));
	}
}
