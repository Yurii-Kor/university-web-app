package ua.foxminded.university.service.dto.request.lesson;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static final OffsetDateTime NOW     = OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime PLUS_1H = NOW.plusHours(1);

    private static final Long TID = 100L;
    private static final Long CID = 10L;
    private static final Long GID = 20L;

    private static final String ROOM_VALID     = "B-105";
    private static final String ROOM_LOWER     = "b-105";
    private static final String ROOM_NO_DASH   = "B105";
    private static final String ROOM_SHORT_DIG = "B-12";
    private static final String ROOM_EMPTY     = "";
    private static final String ROOM_SPACED    = "B-105 ";

    @Autowired
    private Validator validator;

    // -------- VALID CASES --------

    @ParameterizedTest(name = "[{index}] valid -> t={0}, c={1}, g={2}, start={3}, end={4}, room=\"{5}\", type={6}, desc={7}")
    @MethodSource("validCases")
    @DisplayName("LessonCreateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long teacherId,
                                          Long courseId,
                                          Long groupId,
                                          OffsetDateTime startTime,
                                          OffsetDateTime endTime,
                                          String room,
                                          LessonType lessonType,
                                          String description) {

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
        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, LessonType.LECTURE, "note"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, "note"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_VALID, null, null),
                Arguments.of(TID, CID, GID, PLUS_1H, NOW, ROOM_VALID, null, "note")
        );
    }

    // -------- INVALID CASES --------

    @ParameterizedTest(name = "[{index}] invalid -> t={0}, c={1}, g={2}, start={3}, end={4}, room=\"{5}\" -> field={6}")
    @MethodSource("invalidCases")
    @DisplayName("LessonCreateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long teacherId,
                                                        Long courseId,
                                                        Long groupId,
                                                        OffsetDateTime startTime,
                                                        OffsetDateTime endTime,
                                                        String room,
                                                        String expectedField) {

        var dto = new LessonCreateDto(
                teacherId,
                courseId,
                groupId,
                startTime,
                endTime,
                room,
                null,  
                "note"
        );

        Set<ConstraintViolation<LessonCreateDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(),
                "Expected violations for field '" + expectedField + "', but got none");

        var fields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(1, fields.size(),
                "Expected violations only for field '" + expectedField + "', but got fields: " + fields);
        assertEquals(expectedField, fields.iterator().next(),
                "Violations must be on field '" + expectedField + "'");
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(null, CID,  GID,  NOW, PLUS_1H, ROOM_VALID, "teacherId"),
                Arguments.of(TID,  null, GID,  NOW, PLUS_1H, ROOM_VALID, "courseId"),
                Arguments.of(TID,  CID,  null, NOW, PLUS_1H, ROOM_VALID, "groupId"),

                Arguments.of(TID, CID, GID, null,     PLUS_1H, ROOM_VALID, "startTime"),
                Arguments.of(TID, CID, GID, NOW,      null,    ROOM_VALID, "endTime"),

                Arguments.of(TID, CID, GID, NOW, PLUS_1H, null,          "room"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_EMPTY,    "room"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_LOWER,    "room"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_NO_DASH,  "room"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_SHORT_DIG,"room"),
                Arguments.of(TID, CID, GID, NOW, PLUS_1H, ROOM_SPACED,   "room")
        );
    }
}
