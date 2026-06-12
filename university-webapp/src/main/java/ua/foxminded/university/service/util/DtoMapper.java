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
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.dto.request.lesson.LessonCreateDto;
import ua.foxminded.university.service.dto.request.student.StudentCreateDto;
import ua.foxminded.university.service.dto.request.studygroup.StudyGroupCreateDto;
import ua.foxminded.university.service.dto.request.teacher.TeacherCreateDto;

@Component
public class DtoMapper {

	public Course toCourseEntity(CourseCreateDto dto) {
	    return Course.builder()
	            .id(null)
	            .code(dto.code())
	            .name(dto.name())
	            .description(dto.description())
	            .teacher(Teacher.builder().id(dto.teacherId()).build())
	            .build();
	}

	public List<StudyGroup> mapGroupsToEntities(Collection<StudyGroupCreateDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toGroupEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<StudyGroup> toGroupEntity(StudyGroupCreateDto dto) {
		return Optional.ofNullable(dto).map(d -> StudyGroup.builder().id(null).name(d.name()).build());
	}

	public List<Student> toStudentEntities(Collection<StudentCreateDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toStudentEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Student> toStudentEntity(StudentCreateDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Student.builder()
						.id(null)
						.user(toStudentUser(d))
						.group(StudyGroup.builder().id(d.groupId()).build())
						.enrollmentYear(Optional.ofNullable(d.enrollmentYear()).orElse(Year.now().getValue()))
						.build());
	}

	private AppUser toStudentUser(StudentCreateDto d) {
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

	public List<Teacher> toTeacherEntities(Collection<TeacherCreateDto> drafts) {
		return Optional.ofNullable(drafts)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::toTeacherEntity)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<Teacher> toTeacherEntity(TeacherCreateDto dto) {
		return Optional.ofNullable(dto)
				.map(d -> Teacher.builder()
						.id(null)
						.user(toTeacherUser(d))
						.academicRank(d.academicRank())
						.office(d.office())
						.build());
	}

	private AppUser toTeacherUser(TeacherCreateDto d) {
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

	public Optional<Lesson> toLessonEntity(LessonCreateDto dto) {
        return Optional.ofNullable(dto)
                .map(d -> Lesson.builder()
                        .id(null)
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
