package ua.foxminded.university.web.group.cards;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;

@RequiredArgsConstructor
public enum GroupsCardsPageMode {

    ADMIN(
            "Groups",
            "All study groups in the system.",
            true,
            true
    ),
    TEACHER(
            "My groups",
            "Groups where you teach.",
            false,
            false
    );

    private final String pageTitle;
    private final String pageSubtitle;
    private final boolean showAdminActions;
    private final boolean showCreateButton;

    public GroupsCardsPageModel toPage(
            Page<GroupCardView> groupsPage) {

        return new GroupsCardsPageModel(
                pageTitle,
                pageSubtitle,
                groupsPage.getContent(),
                showAdminActions,
                showCreateButton,
                groupsPage.getNumber(),
                groupsPage.getTotalPages(),
                groupsPage.hasPrevious(),
                groupsPage.hasNext());
    }

    public static GroupsCardsPageMode fromRoleKey(
            String roleKey) {

        return switch (roleKey) {
            case "admin" -> ADMIN;
            case "teacher" -> TEACHER;
            default -> throw new AccessDeniedException(
                    "Unsupported groups cards role="
                            + roleKey);
        };
    }
}