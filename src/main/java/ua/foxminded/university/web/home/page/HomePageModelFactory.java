package ua.foxminded.university.web.home.page;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.web.home.content.UserStoryCatalog;
import ua.foxminded.university.web.home.content.dto.UserStoryDefinition;
import ua.foxminded.university.web.home.content.dto.UserStoryRoleDefinition;

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

    private static final List<UserRole> ROLE_ORDER =
            List.of(
                    UserRole.STUDENT,
                    UserRole.TEACHER,
                    UserRole.ADMIN);

    private final UserStoryCatalog userStoryCatalog;

    public HomePageModel create() {
        var features = userStoryCatalog.getDefinitions()
                .stream()
                .map(this::toView)
                .toList();

        return new HomePageModel(
                PAGE_TITLE,
                HEADING,
                DESCRIPTION,
                features);
    }

    private UserStoryFeatureView toView(
            UserStoryDefinition definition) {

        List<String> commonStories =
                definition.common() == null
                        ? List.of()
                        : copyStories(
                                definition.common().stories());

        var roles = ROLE_ORDER.stream()
                .map(role ->
                        toView(
                                role,
                                definition.roles().get(role)))
                .toList();

        return new UserStoryFeatureView(
                definition.id(),
                definition.order(),
                definition.title(),
                definition.description(),
                commonStories,
                roles);
    }

    private UserStoryRoleView toView(
            UserRole role,
            UserStoryRoleDefinition definition) {

        return new UserStoryRoleView(
                toDisplayName(role),
                definition.available(),
                definition.scope(),
                definition.reason(),
                copyStories(definition.stories()));
    }

    private List<String> copyStories(
            List<String> stories) {

        return stories == null
                ? List.of()
                : List.copyOf(stories);
    }

    private String toDisplayName(UserRole role) {
        var name = role.name();

        return name.charAt(0)
                + name.substring(1)
                        .toLowerCase(Locale.ROOT);
    }
}