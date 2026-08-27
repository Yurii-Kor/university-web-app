package ua.foxminded.university.service.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;

@Component
public class DtoMapper {

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

	public StudyGroup toStudyGroupEntity(
			StudyGroupCreateDto dto) {

		return StudyGroup.builder()
				.id(null)
				.name(dto.name())
				.build();
	}

	public Student toStudentEntity(
			StudentCreateDto dto) {

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

	private AppUser toStudentUser(
			StudentCreateDto dto) {

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

	public List<Teacher> toTeacherEntities(
			Collection<TeacherCreateDto> drafts) {

		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(java.util.Objects::nonNull)
				.map(this::toTeacherEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Teacher> toTeacherEntity(
			TeacherCreateDto dto) {

		return Optional.ofNullable(dto)
				.map(d -> Teacher.builder()
						.id(null)
						.user(toTeacherUser(d))
						.academicRank(d.academicRank())
						.office(d.office())
						.build());
	}

	private AppUser toTeacherUser(
			TeacherCreateDto dto) {

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

	public AppUser toAppUserEntity(
			AppUserCreateDto dto) {

		return AppUser.builder()
				.id(null)
				.email(dto.email())
				.password(dto.newPassword())
				.firstName(dto.firstName())
				.lastName(dto.lastName())
				.enabled(true)
				.build();
	}

	public Optional<Lesson> toLessonEntity(
			LessonCreateDto dto) {

		return Optional.ofNullable(dto)
				.map(d -> Lesson.builder()
						.id(null)
						.course(
								Course.builder()
										.id(d.courseId())
										.build())
						.group(
								StudyGroup.builder()
										.id(d.groupId())
										.build())
						.startTime(d.startTime())
						.endTime(d.endTime())
						.room(d.room())
						.lessonType(d.lessonType())
						.description(d.description())
						.build());
	}
}