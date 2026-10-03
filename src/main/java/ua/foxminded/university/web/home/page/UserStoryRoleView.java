package ua.foxminded.university.web.home.page;

import java.util.List;

public record UserStoryRoleView(
        String name,
        boolean available,
        String scope,
        String reason,
        List<String> stories
) {}
