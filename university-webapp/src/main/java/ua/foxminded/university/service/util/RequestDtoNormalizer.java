package ua.foxminded.university.service.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ua.foxminded.university.service.dto.request.AppUserDto;
import ua.foxminded.university.service.dto.request.CourseDto;
import ua.foxminded.university.service.dto.request.LessonDto;
import ua.foxminded.university.service.dto.request.StudentDto;
import ua.foxminded.university.service.dto.request.StudyGroupDto;
import ua.foxminded.university.service.dto.request.TeacherDto;

@Component
public class RequestDtoNormalizer {

	public List<CourseDto> normalizeAll(Collection<CourseDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalize)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<CourseDto> normalize(CourseDto d) {
		return Optional.ofNullable(d)
				.map(dto -> new CourseDto(dto.id(), trimUpper(dto.code()), trim(dto.name()), trim(dto.description()),
						dto.teacherId()));
	}

	public List<StudyGroupDto> normalizeGroups(Collection<StudyGroupDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeGroup)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<StudyGroupDto> normalizeGroup(StudyGroupDto d) {
		return Optional.ofNullable(d).map(dto -> new StudyGroupDto(dto.id(), trimUpper(dto.name())));
	}

	public List<StudentDto> normalizeStudents(Collection<StudentDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeStudent)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<StudentDto> normalizeStudent(StudentDto d) {
		return Optional.ofNullable(d)
				.map(dto -> new StudentDto(trimLower(dto.email()), dto.password(), trim(dto.firstName()),
						trim(dto.lastName()), dto.groupId(), dto.enrollmentYear()));
	}

	public List<TeacherDto> normalizeTeachers(Collection<TeacherDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeTeacher)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<TeacherDto> normalizeTeacher(TeacherDto d) {
		return Optional.ofNullable(d)
				.map(dto -> new TeacherDto(dto.id(), trimLower(dto.email()), dto.password(), trim(dto.firstName()),
						trim(dto.lastName()), dto.academicRank(), trimUpper(dto.office())));
	}

	public List<AppUserDto> normalizeUsers(Collection<AppUserDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeUser)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<AppUserDto> normalizeUser(AppUserDto d) {
		return Optional.ofNullable(d)
				.map(dto -> new AppUserDto(dto.id(), trimLower(dto.email()), dto.newPassword(), dto.currentPassword(),
						trim(dto.firstName()), trim(dto.lastName())));
	}

	public List<LessonDto> normalizeScheduleEntries(Collection<LessonDto> dtos) {
		return Optional.ofNullable(dtos)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeScheduleEntry)
				.flatMap(Optional::stream)
				.toList();
	}

	public Optional<LessonDto> normalizeScheduleEntry(LessonDto d) {
		return Optional.ofNullable(d)
				.map(dto -> new LessonDto(dto.id(), dto.teacherId(), dto.courseId(), dto.groupId(),
						dto.startTime(), dto.endTime(), trimUpper(dto.room()), dto.lessonType(),
						trim(dto.description())));
	}

	private String trim(String s) {
		return Optional.ofNullable(s).map(String::trim).orElse(null);
	}

	private String trimUpper(String s) {
		return Optional.ofNullable(s).map(String::trim).map(t -> t.isEmpty() ? t : t.toUpperCase()).orElse(null);
	}

	private String trimLower(String s) {
		return Optional.ofNullable(s).map(String::trim).map(t -> t.isEmpty() ? t : t.toLowerCase()).orElse(null);
	}
}
