package ua.foxminded.university.web.lesson.page;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;

@RequiredArgsConstructor
public enum SchedulePageMode {

	TEACHER(
			"My schedule",
			"Your teaching schedule.",
			false,
			true,
			true
	),

	STUDENT(
			"My schedule",
			"Your study group schedule.",
			true,
			false,
			false
	);

	private final String pageTitle;
	private final String pageSubtitle;
	private final boolean showTeacherInfo;
	private final boolean showGroupInfo;
	private final boolean allowManage;

	public WeeklySchedulePageModel toPage(
			LocalDate weekStart,
			LocalDate weekEnd,
			List<ScheduleDayModel> days) {

		return new WeeklySchedulePageModel(
				pageTitle,
				pageSubtitle,
				weekStart,
				weekEnd,
				weekStart.minusWeeks(1),
				weekStart.plusWeeks(1),
				days,
				showTeacherInfo,
				showGroupInfo,
				allowManage);
	}

	public DailySchedulePageModel toDailyPage(
			LocalDate date,
			boolean today,
			List<LessonScheduleItemView> lessons) {

		return new DailySchedulePageModel(
				pageTitle,
				pageSubtitle,
				date,
				date.minusDays(1),
				date.plusDays(1),
				today,
				lessons,
				showTeacherInfo,
				showGroupInfo,
				allowManage);
	}

	public static SchedulePageMode fromRoleKey(
			String roleKey) {

		return switch (roleKey) {
			case "teacher" -> TEACHER;
			case "student" -> STUDENT;
			default -> throw new AccessDeniedException(
					"Unsupported schedule role=" + roleKey);
		};
	}
}