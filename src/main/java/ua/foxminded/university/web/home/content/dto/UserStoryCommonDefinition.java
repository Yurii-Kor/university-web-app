package ua.foxminded.university.web.home.content.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record UserStoryCommonDefinition(

        List<@NotBlank String> stories
) {
}