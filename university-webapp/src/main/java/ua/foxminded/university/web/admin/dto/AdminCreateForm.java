package ua.foxminded.university.web.admin.dto;

public record AdminCreateForm(
    String email,
    String firstName,
    String lastName,
    String newPassword,
    String confirmPassword
) {}
