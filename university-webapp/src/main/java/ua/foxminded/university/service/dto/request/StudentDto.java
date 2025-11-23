package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.*;

public record StudentDto(
		@NotBlank
		@Email
		@Size(max = 255) 
		String email,
		
        @NotBlank @Size(min = 8, max = 100)
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{Punct}_])(?=\\S+$).{8,100}$", 
				message = "Password must be 8–100 chars, include upper/lowercase, digit, special, and contain no spaces"
		)
		String password,
		
		@NotBlank
        @Size(max = 64)
		@Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$")
		String firstName,
		
		@NotBlank
        @Size(max = 64)
		@Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$")
		String lastName,
		
        @NotNull 
        Long groupId,
        
        @Min(2000) @Max(2100) 
		Integer enrollmentYear
) {}
