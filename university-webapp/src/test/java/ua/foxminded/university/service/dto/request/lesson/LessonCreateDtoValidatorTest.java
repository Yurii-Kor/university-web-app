package ua.foxminded.university.service.dto.request.lesson;

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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class LessonCreateDtoValidatorTest {

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

	@ParameterizedTest(name = "[{index}] teacherId={0}, courseId={1}, groupId={2}, start={3}, end={4}, room=\"{5}\", type={6}, desc={7} -> valid={8}")
	@MethodSource("cases")
	@DisplayName("LessonCreateDto: bean validation")
	void validate(Long teacherId,
				  Long courseId,
				  Long groupId,
				  OffsetDateTime startTime,
				  OffsetDateTime endTime,
				  String room,
				  LessonType lessonType,
				  String description,
				  boolean shouldPass) {

		var dto = new LessonCreateDto(
				teacherId,
				courseId,
				groupId,
				startTime,
				endTime,
				room,
				lessonType,
				description
		);

		Set<ConstraintViolation<LessonCreateDto>> violations = validator.validate(dto);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, LessonType.LECTURE, "note", true),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", true),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, null, true),
				Arguments.of(TID, CID, GID, PLUS_1H, NOW, ROOM_VALID, null, "note", true),

				Arguments.of(null, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", false), 
				Arguments.of(TID, null, GID, NOW, PLUS_1H, ROOM_VALID, null, "note", false), 
				Arguments.of(TID, CID, null, NOW, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(TID, CID, GID, null, PLUS_1H, ROOM_VALID, null, "note", false),
				Arguments.of(TID, CID, GID, NOW, null, ROOM_VALID, null, "note", false),

				Arguments.of(TID, CID, GID, NOW, PLUS_1H, null,       null, "note", false),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_EMPTY, null, "note", false),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_LOWER, null, "note", false),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_NO_DASH, null, "note", false),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_SHORT_DIG, null, "note", false),
				Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_SPACED, null, "note", false)
		);
	}
}

