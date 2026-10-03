package ua.foxminded.university.web.home.page;

import java.util.List;

public record UserStoryFeatureView(
        String id,
        int order,
        String title,
        String description,
        List<String> commonStories,
        List<UserStoryRoleView> roles
) {
}