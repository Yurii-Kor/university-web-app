package ua.foxminded.university.service.dto.request.student;

import jakarta.validation.constraints.*;
import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record StudentCreateDto(
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
        Long groupId,
        
        @Min(ENROLLMENT_YEAR_MIN) @Max(ENROLLMENT_YEAR_MAX) 
		Integer enrollmentYear
) {}
