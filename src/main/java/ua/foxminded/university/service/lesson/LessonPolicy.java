package ua.foxminded.university.service.lesson;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;

@Component
@RequiredArgsConstructor
public class LessonPolicy {

	private final CourseRepository courseRepository;
	private final TeacherRepository teacherRepository;
	private final Clock clock;

	public List<LessonPolicyViolation> validateForCreate(
			Lesson lesson,
			long teacherId) {

		var violations = new ArrayList<LessonPolicyViolation>();

		validateCourseBelongsToTeacher(
				lesson,
				teacherId,
				"courseId",
				violations);

		validateEndAfterStart(
				lesson,
				violations);

		validateLessonIsFuture(
				lesson,
				"startTime",
				"Lesson start time must be in the future.",
				violations);

		validateGroupAttachedToCourse(
				lesson,
				"groupId",
				violations);

		validateRoomIsNotTeacherOffice(
				lesson,
				violations);

		return List.copyOf(violations);
	}

	public List<LessonPolicyViolation> validateCanUpdate(
			Lesson lesson,
			long teacherId) {

		var violations = new ArrayList<LessonPolicyViolation>();

		validateLessonBelongsToTeacher(
				lesson,
				teacherId,
				violations);

		validateLessonIsFuture(
				lesson,
				null,
				"Only future lessons can be modified.",
				violations);

		validateCourseBelongsToTeacher(
				lesson,
				teacherId,
				null,
				violations);

		validateGroupAttachedToCourse(
				lesson,
				null,
				violations);

		return List.copyOf(violations);
	}

	public List<LessonPolicyViolation> validateCanModify(
			Lesson lesson,
			long teacherId) {

		var violations = new ArrayList<LessonPolicyViolation>();

		validateLessonBelongsToTeacher(
				lesson,
				teacherId,
				violations);

		validateLessonIsFuture(
				lesson,
				null,
				"Only future lessons can be modified.",
				violations);

		return List.copyOf(violations);
	}

	public List<LessonPolicyViolation> validateAfterUpdate(
			Lesson lesson,
			long teacherId) {

		var violations = new ArrayList<LessonPolicyViolation>();

		validateEndAfterStart(
				lesson,
				violations);

		validateLessonIsFuture(
				lesson,
				"startTime",
				"Lesson start time must be in the future.",
				violations);

		validateRoomIsNotTeacherOffice(
				lesson,
				violations);

		return List.copyOf(violations);
	}

	private void validateLessonBelongsToTeacher(
			Lesson lesson,
			long teacherId,
			List<LessonPolicyViolation> violations) {

		var assignedTeacherId =
				lesson.getAssignedTeacher().getId();

		if (assignedTeacherId != teacherId) {
			addViolation(
					violations,
					null,
					"Lesson does not belong to the teacher: lessonId=%d, teacherId=%d"
							.formatted(
									lesson.getId(),
									teacherId));
		}
	}

	private void validateCourseBelongsToTeacher(
			Lesson lesson,
			long teacherId,
			String field,
			List<LessonPolicyViolation> violations) {

		var courseId =
				lesson.getCourse().getId();

		if (!courseRepository.existsByIdAndTeacher_Id(
				courseId,
				teacherId)) {

			addViolation(
					violations,
					field,
					"Course does not belong to the teacher: courseId=%d, teacherId=%d"
							.formatted(
									courseId,
									teacherId));
		}
	}

	private void validateGroupAttachedToCourse(
			Lesson lesson,
			String field,
			List<LessonPolicyViolation> violations) {

		var courseId =
				lesson.getCourse().getId();

		var groupId =
				lesson.getGroup().getId();

		if (!courseRepository.existsByIdAndGroups_Id(
				courseId,
				groupId)) {

			addViolation(
					violations,
					field,
					"Study group is not assigned to the course: groupId=%d, courseId=%d"
							.formatted(
									groupId,
									courseId));
		}
	}

	private void validateEndAfterStart(
			Lesson lesson,
			List<LessonPolicyViolation> violations) {

		var startTime =
				lesson.getStartTime();

		var endTime =
				lesson.getEndTime();

		if (!endTime.isAfter(startTime)) {
			addViolation(
					violations,
					"endTime",
					"Lesson end time must be after start time.");
		}
	}

	private void validateLessonIsFuture(
			Lesson lesson,
			String field,
			String message,
			List<LessonPolicyViolation> violations) {

		if (!lesson.getStartTime()
				.isAfter(OffsetDateTime.now(clock))) {

			addViolation(
					violations,
					field,
					message);
		}
	}

	private void validateRoomIsNotTeacherOffice(
			Lesson lesson,
			List<LessonPolicyViolation> violations) {

		var room =
				Optional.ofNullable(lesson.getRoom())
						.filter(value -> !value.isBlank());

		if (room.isEmpty()) {
			addViolation(
					violations,
					"room",
					"Lesson room must be specified.");

			return;
		}

		if (teacherRepository.existsByOfficeIgnoreCase(
				room.get())) {

			addViolation(
					violations,
					"room",
					"Room is reserved as a teacher office: room="
							+ room.get());
		}
	}

	private void addViolation(
			List<LessonPolicyViolation> violations,
			String field,
			String message) {

		violations.add(
				new LessonPolicyViolation(
						field,
						message));
	}
}