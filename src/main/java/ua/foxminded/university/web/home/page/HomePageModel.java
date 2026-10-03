package ua.foxminded.university.web.home.page;

import java.util.List;

public record HomePageModel(
        String pageTitle,
        String heading,
        String description,
        List<UserStoryFeatureView> features
) {}
