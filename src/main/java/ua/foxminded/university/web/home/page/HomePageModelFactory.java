package ua.foxminded.university.web.home.page;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.home.content.UserStoryCatalog;

@Component
@RequiredArgsConstructor
public class HomePageModelFactory {

    private static final String PAGE_TITLE = "Home";

    private static final String HEADING =
            "Academic management for students, teachers and administrators";

    private static final String DESCRIPTION =
            "Explore the application's role-based functionality below. "
                    + "Each section describes the capabilities available "
                    + "to different types of accounts.";

    private final UserStoryCatalog userStoryCatalog;

    public HomePageModel create() {
        return new HomePageModel(
                PAGE_TITLE,
                HEADING,
                DESCRIPTION,
                userStoryCatalog.getFeatures());
    }
}