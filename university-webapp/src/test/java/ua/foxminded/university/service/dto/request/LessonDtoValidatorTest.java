package ua.foxminded.university.service.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class LessonDtoValidatorTest {

	private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime PLUS_1H = NOW.plusHours(1);

	private static final Long TID = 100L;
	private static final Long CID = 10L;
	private static final Long GID = 20L;

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_NO_DASH = "B105";
	private static final String ROOM_SHORT_DIG = "B-12";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = "[{index}] group={0}, id={1}, teacherId={2}, courseId={3}, groupId={4}, start={5}, end={6}, room=\"{7}\", type={8}, desc={9} -> valid={10}")
	@MethodSource("cases")
	@DisplayName("ScheduleEntryDto: bean validation across groups (OnCreate / OnUpdateSelf)")
	void validate(Class<?> group, Long id, Long teacherId, Long courseId, Long groupId, OffsetDateTime startTime,
			OffsetDateTime endTime, String room, LessonType lessonType, String description, boolean shouldPass) {

		LessonDto dto = new LessonDto(id, teacherId, courseId, groupId, startTime, endTime, room,
				lessonType, description);

		Set<ConstraintViolation<LessonDto>> violations = validator.validate(dto, group);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(
				
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, LessonType.LECTURE, "note", true),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", true),
				Arguments.of(OnCreate.class, 1L, TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, null, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, null, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, null, NOW, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, null, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, null, ROOM_VALID, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, null, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_EMPTY, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_LOWER, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_NO_DASH, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_SHORT_DIG, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, NOW, PLUS_1H, ROOM_SPACED, null, "note", false),
				Arguments.of(OnCreate.class, null, TID, CID, GID, PLUS_1H, NOW, ROOM_VALID, null, "note", true),

				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, null, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, NOW, null, null, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, PLUS_1H, null, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, PLUS_1H, NOW, null, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, null, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, ROOM_VALID, null, null, true),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, ROOM_EMPTY, null, null, false),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, ROOM_LOWER, null, null, false),
				Arguments.of(OnUpdateSelf.class, 1L, TID, null, null, null, null, ROOM_SPACED, null, null, false),
				Arguments.of(OnUpdateSelf.class, null, TID, null, null, null, null, null, null, null, false),
				Arguments.of(OnUpdateSelf.class, 1L, null, null, null, null, null, null, null, null, false));
	}
}
