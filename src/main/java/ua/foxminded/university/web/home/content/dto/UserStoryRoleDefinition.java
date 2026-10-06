package ua.foxminded.university.web.home.content.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserStoryRoleDefinition(

        @NotNull
        Boolean available,

        String scope,

        String reason,

        List<@NotBlank String> stories
) {
}