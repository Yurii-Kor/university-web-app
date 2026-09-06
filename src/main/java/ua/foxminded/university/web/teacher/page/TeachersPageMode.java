package ua.foxminded.university.web.teacher.page;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;

@RequiredArgsConstructor
public enum TeachersPageMode {

	ADMIN(
			"Teachers",
			"All teachers in the system.",
			true,
			true
	),

	TEACHER(
			"Teachers",
			"Your colleagues.",
			false,
			false
	),

	STUDENT(
			"My teachers",
			"Teachers assigned to your group.",
			false,
			false
	);

	private final String pageTitle;
	private final String pageSubtitle;
	private final boolean allowEdit;
	private final boolean showCreateButton;

	public TeachersPageModel toPage(
			Page<TeacherDirectoryCardView> teachersPage) {

		return new TeachersPageModel(
				pageTitle,
				pageSubtitle,
				teachersPage.getContent(),
				allowEdit,
				showCreateButton,
				teachersPage.getNumber(),
				teachersPage.getTotalPages(),
				teachersPage.hasPrevious(),
				teachersPage.hasNext());
	}

	public static TeachersPageMode fromRoleKey(
			String roleKey) {

		return switch (roleKey) {
			case "admin" -> ADMIN;
			case "teacher" -> TEACHER;
			case "student" -> STUDENT;
			default -> throw new AccessDeniedException(
					"Unsupported role=" + roleKey);
		};
	}
}