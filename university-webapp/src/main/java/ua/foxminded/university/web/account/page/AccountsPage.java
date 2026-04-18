package ua.foxminded.university.web.account.page;

import java.util.List;

import ua.foxminded.university.model.repository.dto.StudentCardView;
import ua.foxminded.university.model.repository.dto.TeacherCardView;

public record AccountsPage(
        String pageTitle,
        String pageSubtitle,
        String currentView,
        int currentPage,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        List<StudentCardView> students,
        List<TeacherCardView> teachers
) {}