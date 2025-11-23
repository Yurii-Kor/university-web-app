package ua.foxminded.university.service.util;

import java.time.Year;
import java.util.*;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.dto.request.AppUserDto;
import ua.foxminded.university.service.dto.request.CourseDto;
import ua.foxminded.university.service.dto.request.LessonDto;
import ua.foxminded.university.service.dto.request.StudentDto;
import ua.foxminded.university.service.dto.request.StudyGroupDto;
import ua.foxminded.university.service.dto.request.TeacherDto;

@Component
public class DtoMapper {

	public List<Course> toCourseEntities(Collection<CourseDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toCourseEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Course> toCourseEntity(CourseDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Course.builder()
						.id(null)
						.code(d.code())
						.name(d.name())
						.description(d.description())
						.teacher(Teacher.builder().id(d.teacherId()).build())
						.build());
	}

	public List<StudyGroup> mapGroupsToEntities(Collection<StudyGroupDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toGroupEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<StudyGroup> toGroupEntity(StudyGroupDto dto) {
		return Optional.ofNullable(dto).map(d -> StudyGroup.builder().id(null).name(dto.name()).build());
	}

	public List<Student> toStudentEntities(Collection<StudentDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toStudentEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Student> toStudentEntity(StudentDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Student.builder()
						.id(null)
						.user(toStudentUser(d))
						.group(StudyGroup.builder().id(d.groupId()).build())
						.enrollmentYear(Optional.ofNullable(d.enrollmentYear()).orElse(Year.now().getValue()))
						.build());
	}

	private AppUser toStudentUser(StudentDto d) {
		return AppUser.builder()
				.id(null)
				.email(d.email())
				.password(d.password())
				.role(UserRole.STUDENT)
				.firstName(d.firstName())
				.lastName(d.lastName())
				.enabled(true)
				.build();
	}

	public List<Teacher> toTeacherEntities(Collection<TeacherDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toTeacherEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Teacher> toTeacherEntity(TeacherDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Teacher.builder()
						.id(null)
						.user(toTeacherUser(d))
						.academicRank(d.academicRank())
						.office(d.office())
						.build());
	}

	private AppUser toTeacherUser(TeacherDto d) {
		return AppUser.builder()
				.id(null)
				.email(d.email())
				.password(d.password())
				.role(UserRole.TEACHER)
				.firstName(d.firstName())
				.lastName(d.lastName())
				.enabled(true)
				.build();
	}

	public List<AppUser> toAppUserEntities(Collection<AppUserDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toAppUserEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<AppUser> toAppUserEntity(AppUserDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> AppUser.builder()
						.id(null)
						.email(d.email())
						.password(d.newPassword())
						.firstName(d.firstName())
						.lastName(d.lastName())
						.enabled(true)
						.build());
	}

	public Optional<Lesson> toScheduleEntryEntity(LessonDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Lesson.builder()
						.id(d.id())
						.course(Course.builder().id(d.courseId()).build())
						.group(StudyGroup.builder().id(d.groupId()).build())
						.startTime(d.startTime())
						.endTime(d.endTime())
						.room(d.room())
						.lessonType(d.lessonType())
						.description(d.description())
						.build());
	}
}
