package ua.foxminded.university.web.group.cards;

import java.util.List;

import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;

public record GroupsCardsPageModel(
        String pageTitle,
        String pageSubtitle,
        List<GroupCardView> groups,
        boolean showAdminActions,
        boolean showCreateButton,
        int currentPage,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {}
