package ua.foxminded.university.web.student.cards;

import java.util.List;

import ua.foxminded.university.model.persistence.student.projection.StudentCardProjection;

public record StudentsCardsPageModel(
        String pageTitle,
        String pageSubtitle,
        List<StudentCardProjection> students,
        boolean showGroupInfo,
        boolean showEnrollmentYear,
        boolean showAdminActions,
        boolean showCreateButton,
        int currentPage,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
}