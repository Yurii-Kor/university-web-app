package ua.foxminded.university.web.group.details;

import org.springframework.security.access.AccessDeniedException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupDetailsPageMode {

	STUDENT(
			"My group",
			"Your study group.",
			"groups/details/student"
	);

	private final String pageTitle;
	private final String pageSubtitle;
	private final String viewName;

	public static GroupDetailsPageMode fromRoleKey(
			String roleKey) {

		return switch (roleKey) {
			case "student" -> STUDENT;
			default -> throw new AccessDeniedException(
					"Unsupported group details role="
							+ roleKey);
		};
	}
}