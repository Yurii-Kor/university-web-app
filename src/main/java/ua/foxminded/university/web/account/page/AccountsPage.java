package ua.foxminded.university.web.account.page;

import java.util.List;

import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherRoleChangeCardView;

public record AccountsPage(
        String pageTitle,
        String pageSubtitle,
        String currentView,
        int currentPage,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        List<StudentCardView> students,
        List<TeacherRoleChangeCardView> teachers
) {}