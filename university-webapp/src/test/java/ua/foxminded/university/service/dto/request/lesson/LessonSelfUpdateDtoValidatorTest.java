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
class LessonSelfUpdateDtoValidatorTest {

	private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime PLUS_1H = NOW.plusHours(1);

	private static final Long TID = 100L;

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = "[{index}] id={0}, teacherId={1}, courseId={2}, groupId={3}, start={4}, end={5}, room=\"{6}\", type={7}, desc={8} -> valid={9}")
	@MethodSource("cases")
	@DisplayName("LessonSelfUpdateDto: bean validation")
	void validate(Long id, Long teacherId, Long courseId, Long groupId, OffsetDateTime startTime,
			OffsetDateTime endTime, String room, LessonType lessonType, String description, boolean shouldPass) {

		var dto = new LessonSelfUpdateDto(id, teacherId, courseId, groupId, startTime, endTime, room, lessonType,
				description);

		Set<ConstraintViolation<LessonSelfUpdateDto>> violations = validator.validate(dto);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(1L, TID, null, null, null, null, null, null, null, true),
				Arguments.of(1L, TID, null, null, NOW, null, null, null, null, true),
				Arguments.of(1L, TID, null, null, null, PLUS_1H, null, null, null, true),
				Arguments.of(1L, TID, null, null, PLUS_1H, NOW, null, null, null, true),
				Arguments.of(1L, TID, null, null, null, null, null, null, "note", true),

				Arguments.of(1L, TID, null, null, null, null, ROOM_VALID, null, null, true),

				Arguments.of(1L, TID, null, null, null, null, ROOM_EMPTY, null, null, false),
				Arguments.of(1L, TID, null, null, null, null, ROOM_LOWER, null, null, false),
				Arguments.of(1L, TID, null, null, null, null, ROOM_SPACED, null, null, false),

				Arguments.of(null, TID, null, null, null, null, null, null, null, false),
				Arguments.of(1L, null, null, null, null, null, null, null, null, false));
	}
}
