package ua.foxminded.university.service.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.exception.RequestedSlot;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;

@Component
@RequiredArgsConstructor
public class DtoMapper {

	private final Clock clock;

	public Course toCourseEntity(CourseCreateDto dto) {
		return Course.builder()
				.id(null)
				.code(dto.code())
				.name(dto.name())
				.description(dto.description())
				.teacher(
						Teacher.builder()
								.id(dto.teacherId())
								.build())
				.build();
	}

	public StudyGroup toStudyGroupEntity(StudyGroupCreateDto dto) {
		return StudyGroup.builder()
				.id(null)
				.name(dto.name())
				.build();
	}

	public Student toStudentEntity(StudentCreateDto dto) {
		return Student.builder()
				.id(null)
				.user(toStudentUser(dto))
				.group(
						StudyGroup.builder()
								.id(dto.groupId())
								.build())
				.enrollmentYear(dto.enrollmentYear())
				.build();
	}

	private AppUser toStudentUser(StudentCreateDto dto) {
		return AppUser.builder()
				.id(null)
				.email(dto.email())
				.password(dto.password())
				.role(UserRole.STUDENT)
				.firstName(dto.firstName())
				.lastName(dto.lastName())
				.enabled(true)
				.build();
	}

	public Teacher toTeacherEntity(TeacherCreateDto dto) {
		return Teacher.builder()
				.id(null)
				.user(toTeacherUser(dto))
				.academicRank(dto.academicRank())
				.office(dto.office())
				.build();
	}

	private AppUser toTeacherUser(TeacherCreateDto dto) {
		return AppUser.builder()
				.id(null)
				.email(dto.email())
				.password(dto.password())
				.role(UserRole.TEACHER)
				.firstName(dto.firstName())
				.lastName(dto.lastName())
				.enabled(true)
				.build();
	}

	public AppUser toAppUserEntity(AppUserCreateDto dto) {
		return AppUser.builder()
				.id(null)
				.email(dto.email())
				.password(dto.newPassword())
				.firstName(dto.firstName())
				.lastName(dto.lastName())
				.enabled(true)
				.build();
	}

	public Lesson toLessonEntity(
			LessonCreateDto dto,
			Course course,
			StudyGroup group) {

		return Lesson.builder()
				.id(null)
				.course(course)
				.group(group)
				.assignedTeacher(course.getTeacher())
				.startTime(toOffsetDateTime(
						dto.date(),
						dto.startTime()))
				.endTime(toOffsetDateTime(
						dto.date(),
						dto.endTime()))
				.room(normalizeRoom(dto.room()))
				.lessonType(
						Optional.ofNullable(dto.lessonType())
								.orElse(LessonType.OTHER))
				.description(normalizeDescription(dto.description()))
				.build();
	}

	public void applyLessonUpdate(
			Lesson lesson,
			LessonUpdateDto dto) {

		lesson.setStartTime(toOffsetDateTime(
				dto.date(),
				dto.startTime()));

		lesson.setEndTime(toOffsetDateTime(
				dto.date(),
				dto.endTime()));

		lesson.setRoom(
				normalizeRoom(dto.room()));

		lesson.setLessonType(
				dto.lessonType());

		lesson.setDescription(
				normalizeDescription(dto.description()));
	}

	public RequestedSlot toRequestedSlot(
			Lesson lesson,
			long teacherId) {

		return new RequestedSlot(
				lesson.getGroup().getId(),
				lesson.getCourse().getId(),
				teacherId,
				lesson.getStartTime(),
				lesson.getEndTime(),
				lesson.getRoom(),
				lesson.getLessonType(),
				lesson.getDescription());
	}

	private OffsetDateTime toOffsetDateTime(
			LocalDate date,
			LocalTime time) {

		return date.atTime(time)
				.atZone(clock.getZone())
				.toOffsetDateTime();
	}

	private String normalizeRoom(String room) {
		return Optional.ofNullable(room)
				.map(String::trim)
				.orElse(null);
	}

	private String normalizeDescription(String description) {
		return Optional.ofNullable(description)
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.orElse(null);
	}
}