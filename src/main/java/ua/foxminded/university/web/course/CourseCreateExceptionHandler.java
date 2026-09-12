package ua.foxminded.university.web.course;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.service.course.exception.CourseCreateException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = CourseCreateController.class)
@RequiredArgsConstructor
public class CourseCreateExceptionHandler {

	private static final String FORM_ATTRIBUTE = "form";
	private static final String CREATE_VIEW = "courses/create";
	private static final String CREATE_REDIRECT = "redirect:/courses/create";

	private final ObjectProvider<TeacherService> teacherServiceProvider;
	private final ExceptionMessageReader messageReader;

	@ExceptionHandler(CourseCreateException.class)
	public String handleCourseCreate(
			CourseCreateException ex,
			Model model) {

		model.addAttribute(
				FORM_ATTRIBUTE,
				ex.getForm());

		model.addAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid request."));

		addPageData(
				model);

		return CREATE_VIEW;
	}

	@ExceptionHandler(BindException.class)
	public String handleValidation(
			BindException ex,
			Model model) {

		model.addAllAttributes(
				ex.getBindingResult()
						.getModel());

		addPageData(
				model);

		return CREATE_VIEW;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public String handleBadRequest(
			IllegalArgumentException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid request."));

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public String handleValidation(
			ConstraintViolationException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				messageReader.formatViolations(
						ex));

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public String handleNotFound(
			EntityNotFoundException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Teacher not found."));

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public String handleDbConflict(
			DataIntegrityViolationException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				"Course code or name already exists.");

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(Exception.class)
	public String handleOther(
			Exception ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				"Something went wrong.");

		return CREATE_REDIRECT;
	}

	private void addPageData(
			Model model) {

		model.addAttribute(
				"all_teachers",
				teacherServiceProvider.getObject()
						.listTeacherOptions());
	}

	private CourseCreateDto emptyForm() {

		return new CourseCreateDto(
				null,
				null,
				null,
				null);
	}
}