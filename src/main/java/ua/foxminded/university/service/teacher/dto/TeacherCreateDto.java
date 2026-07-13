package ua.foxminded.university.service.teacher.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.AcademicRank;

public record TeacherCreateDto(

		@NotBlank
        @Email
        @Size(max = EMAIL_MAX)
        String email,

        @NotBlank
        @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
        @Pattern(
            regexp = PASSWORD_REGEX,
            message = PASSWORD_MESSAGE
        )
        String password,

        @NotBlank
        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
		@Pattern(
	        regexp = PERSON_NAME_REGEX,
	        message = PERSON_NAME_MESSAGE
	    )
        String firstName,

        @NotBlank
        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
		@Pattern(
		    regexp = PERSON_NAME_REGEX,
		    message = PERSON_NAME_MESSAGE
		)
        String lastName,

        @NotNull
        AcademicRank academicRank,

        @NotBlank
        @Size(max = ROOM_MAX)
        @Pattern(
            regexp = ROOM_CODE_REGEX,
            message = OFFICE_MESSAGE
        )
        String office
) {}
