package ua.foxminded.university.service.dto.request;

import static org.junit.jupiter.api.Assertions.*;

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

import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateCodes;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class CourseDtoValidatorTest {

	private static final Long VALID_ID = 123L;
	private static final Long TEACHER_ID = 1L;

	private static final String VALID_CODE_LONG = "CSE-ALG-101";
	private static final String VALID_CODE_SHORT = "SEC-303";

	private static final String LOWER = "cse-alg-101";
	private static final String BAD_CODE = "bad";
	private static final String BAD_PREFIX = "C-ALG-101";
	private static final String BAD_MIDLONG = "CSE-ALGORITHMS-101";
	private static final String BAD_NUM_2DIG = "CSE-ALG-10";
	private static final String BAD_NUM_4DIG = "CSE-ALG-1000";

	private static final String NAME_OK = "Operating Systems (Linux)";
	private static final String NAME_EMPTY = "";
	private static final String NAME_BLANK = "   ";
	private static final String NAME_BAD_LEAD_SPACE = " security";
	private static final String NAME_255 = "S".repeat(255);
	private static final String NAME_256 = "S".repeat(256);

	private static final String DESC_NULL = null;
	private static final String DESC_EMPTY = "";
	private static final String DESC_OK_10K = "A".repeat(10_000);
	private static final String DESC_TOO_LONG = "A".repeat(10_001);

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = "[{index}] group={0}, id={1}, code={2}, nameLen={3}, descLen={4} -> valid={6}")
	@MethodSource("cases")
	@DisplayName("CourseDto: bean validation across groups (OnCreate / OnUpdateSelf / OnUpdateCodes)")
	void validate(Class<?> group, Long id, String code, String name, String description, Long teacherId,
			boolean shouldPass) {
		CourseDto dto = new CourseDto(id, code, name, description, teacherId);
		Set<ConstraintViolation<CourseDto>> violations = validator.validate(dto, group);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(
				// -------------------- OnCreate: valid --------------------
				Arguments.of(OnCreate.class, null, VALID_CODE_LONG,  NAME_OK,     DESC_NULL,    TEACHER_ID, true),
	            Arguments.of(OnCreate.class, null, VALID_CODE_SHORT, NAME_OK,     DESC_EMPTY,   TEACHER_ID, true),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG,  NAME_OK,     DESC_OK_10K,  TEACHER_ID, true),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG,  NAME_255,    DESC_NULL,    TEACHER_ID, true),

	            // -------------------- OnCreate: invalid ------------------
	            Arguments.of(OnCreate.class, null, null,            NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, "",              NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, LOWER,           NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, BAD_CODE,        NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, BAD_PREFIX,      NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, BAD_MIDLONG,     NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, BAD_NUM_2DIG,    NAME_OK,    DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, BAD_NUM_4DIG,    NAME_OK,    DESC_NULL,   TEACHER_ID, false),

	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, null,        DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, NAME_EMPTY,  DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, NAME_BLANK,  DESC_NULL,   TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, NAME_256,    DESC_NULL,   TEACHER_ID, false),

	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, NAME_OK,     DESC_TOO_LONG, TEACHER_ID, false),
	            Arguments.of(OnCreate.class, null, VALID_CODE_LONG, NAME_OK,     DESC_NULL,     null,       false),

	            // -------------------- OnUpdateSelf: valid ----------------
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null,       DESC_NULL,    null, true),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_EMPTY, DESC_EMPTY,   null, true),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_BLANK, DESC_OK_10K,  null, true),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_OK,    DESC_NULL,    null, true),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_255,   DESC_EMPTY,   null, true),

	            // -------------------- OnUpdateSelf: invalid --------------
	            Arguments.of(OnUpdateSelf.class, null,     null, NAME_OK,             DESC_NULL,   null, false), 
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_BAD_LEAD_SPACE, DESC_NULL,   null, false),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_256,            DESC_NULL,   null, false),
	            Arguments.of(OnUpdateSelf.class, VALID_ID, null, NAME_OK,             DESC_TOO_LONG, null, false),

	            // -------------------- OnUpdateCodes: valid ---------------
	            Arguments.of(OnUpdateCodes.class, VALID_ID, VALID_CODE_LONG,  null, null, null, true),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, VALID_CODE_SHORT, null, null, null, true),

	            // -------------------- OnUpdateCodes: invalid -------------
	            Arguments.of(OnUpdateCodes.class, null,     VALID_CODE_LONG,  null, null, null, false), 
	            Arguments.of(OnUpdateCodes.class, VALID_ID, null,             null, null, null, false), 
	            Arguments.of(OnUpdateCodes.class, VALID_ID, "",               null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, LOWER,            null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, BAD_CODE,         null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, BAD_PREFIX,       null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, BAD_MIDLONG,      null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, BAD_NUM_2DIG,     null, null, null, false),
	            Arguments.of(OnUpdateCodes.class, VALID_ID, BAD_NUM_4DIG,     null, null, null, false)
	        );
	}
}
