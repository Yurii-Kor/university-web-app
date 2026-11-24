package ua.foxminded.university.web.home.content.dto;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.UserRole;

public record UserStoryDefinition(

        @NotBlank
        @Pattern(regexp = "[a-z0-9-]+")
        String id,

        @NotNull
        @Positive
        Integer order,

        @NotBlank
        String title,

        @NotBlank
        String description,

        @Valid
        UserStoryCommonDefinition common,

        @NotNull
        @Size(min = 3, max = 3)
        Map<UserRole, @NotNull @Valid UserStoryRoleDefinition> roles
) {
}