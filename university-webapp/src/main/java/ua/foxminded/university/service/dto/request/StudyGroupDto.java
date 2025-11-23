package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnRename;

public record StudyGroupDto(
		@NotNull(groups = { OnRename.class })
	    Long id,
	    
        @NotBlank(groups = { OnCreate.class, OnRename.class })
        @Size(max = 255, groups = { OnCreate.class, OnRename.class })
        @Pattern(
            regexp = "^[A-Z]{2}-\\d{3}$",
            message = "Group name must look like 'CS-101'",
            groups = { OnCreate.class, OnRename.class }
        )
        String name
) {}
