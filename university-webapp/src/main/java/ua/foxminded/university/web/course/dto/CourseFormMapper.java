package ua.foxminded.university.web.course.dto;

import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.web.util.DtoFieldNormalizer;

@Component
@RequiredArgsConstructor
public class CourseFormMapper {
	
	private final DtoFieldNormalizer normalizer;
	
	public CourseCreateDto toCreateDto(CourseCreateForm form) {
		form = Optional.ofNullable(form).orElse(new CourseCreateForm("", "", "", null));
		
		return new CourseCreateDto(
				normalizer.normalizeCode(form.code()),
				normalizer.normalizeField(form.name()),
				normalizer.normalizeField(form.description()),
				form.teacherId()
		);
	}
	
	public CourseSelfUpdateDto toSelfUpdateDto(CourseSelfUpdateForm form) {
		form = Optional.ofNullable(form).orElse(new CourseSelfUpdateForm(null, "", "", null));
		
		return new CourseSelfUpdateDto(
				form.id(),
				normalizer.normalizeCode(form.code()),
				normalizer.normalizeField(form.name()),
				form.teacherId()
		);
	}
	
	public CourseDescriptionUpdateDto toDescriptionUpdateDto(CourseDescriptionUpdateForm form) {
		form = Optional.ofNullable(form).orElse(new CourseDescriptionUpdateForm(null, null));
		
		return new CourseDescriptionUpdateDto(
				form.id(),
				normalizer.normalizeField(form.description())
		);
	}
}
