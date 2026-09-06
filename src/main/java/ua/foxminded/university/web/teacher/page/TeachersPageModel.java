package ua.foxminded.university.web.teacher.page;

import java.util.List;

import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;

public record TeachersPageModel(
		String pageTitle,
		String pageSubtitle,
		List<TeacherDirectoryCardView> teachers,
		boolean allowEdit,
		boolean showCreateButton,
		int currentPage,
		int totalPages,
		boolean hasPrevious,
		boolean hasNext
) {
}