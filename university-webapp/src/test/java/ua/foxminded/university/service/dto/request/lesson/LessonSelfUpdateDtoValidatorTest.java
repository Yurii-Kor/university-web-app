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
class LessonSelfUpdateDtoValidatorTest {

    private static final OffsetDateTime NOW     = OffsetDateTime.of(2025, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime PLUS_1H = NOW.plusHours(1);

    private static final Long TID = 100L;

    private static final String ROOM_VALID  = "B-105";
    private static final String ROOM_LOWER  = "b-105";
    private static final String ROOM_EMPTY  = "";
    private static final String ROOM_SPACED = "B-105 ";

    @Autowired
    private Validator validator;

    // ---------- VALID CASES ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, teacherId={1}, start={4}, end={5}, room=\"{6}\", type={7}, desc={8}")
    @MethodSource("validCases")
    @DisplayName("LessonSelfUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id,
                                          Long teacherId,
                                          Long courseId,
                                          Long groupId,
                                          OffsetDateTime startTime,
                                          OffsetDateTime endTime,
                                          String room,
                                          LessonType lessonType,
                                          String description) {

        var dto = new LessonSelfUpdateDto(
                id,
                teacherId,
                courseId,
                groupId,
                startTime,
                endTime,
                room,
                lessonType,
                description
        );

        Set<ConstraintViolation<LessonSelfUpdateDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(1L, TID, null, null, null,      null,     null,       null, null),
                Arguments.of(1L, TID, null, null, NOW,       null,     null,       null, null),
                Arguments.of(1L, TID, null, null, null,      PLUS_1H,  null,       null, null),
                Arguments.of(1L, TID, null, null, PLUS_1H,   NOW,      null,       null, null),
                Arguments.of(1L, TID, null, null, null,      null,     null,       null, "note"),
                Arguments.of(1L, TID, null, null, null,      null,     ROOM_VALID, null, null)
        );
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, teacherId={1}, room=\"{6}\" -> field={9}")
    @MethodSource("invalidCases")
    @DisplayName("LessonSelfUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        Long teacherId,
                                                        Long courseId,
                                                        Long groupId,
                                                        OffsetDateTime startTime,
                                                        OffsetDateTime endTime,
                                                        String room,
                                                        LessonType lessonType,
                                                        String description,
                                                        String expectedField) {

        var dto = new LessonSelfUpdateDto(
                id,
                teacherId,
                courseId,
                groupId,
                startTime,
                endTime,
                room,
                lessonType,
                description
        );

        Set<ConstraintViolation<LessonSelfUpdateDto>> violations = validator.validate(dto);

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
                Arguments.of(1L, TID, null, null, null, null, ROOM_EMPTY,  null, null, "room"),
                Arguments.of(1L, TID, null, null, null, null, ROOM_LOWER,  null, null, "room"),
                Arguments.of(1L, TID, null, null, null, null, ROOM_SPACED, null, null, "room"),

                Arguments.of(null, TID, null, null, null, null, null, null, null, "id"),
                
                Arguments.of(1L,  null, null, null, null, null, null, null, null, "teacherId")
        );
    }
}
