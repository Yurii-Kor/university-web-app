package ua.foxminded.university.web.profile.dto;

public record SelfUpdateForm(
        String email,
        String firstName,
        String lastName
) {}
