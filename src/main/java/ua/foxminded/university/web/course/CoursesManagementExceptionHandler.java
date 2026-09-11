package ua.foxminded.university.web.course;

import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.service.course.exception.CourseSelfUpdateException;
import ua.foxminded.university.service.course.exception.CourseTeacherReassignmentException;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = CoursesManagementController.class)
@RequiredArgsConstructor
public class CoursesManagementExceptionHandler {

	private final ExceptionMessageReader messageReader;

	@ExceptionHandler(CourseTeacherReassignmentException.class)
	public String handleTeacherReassignment(
			CourseTeacherReassignmentException ex,
			HttpServletRequest request,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"courseId",
				ex.courseId());

		ra.addFlashAttribute(
				"err",
				buildTeacherReassignmentMessage(ex));

		ra.addFlashAttribute(
				"teacherReassignmentError",
				ex);

		return redirectToCoursesPage(request);
	}

	@ExceptionHandler(CourseSelfUpdateException.class)
	public String handleSelfUpdate(
			CourseSelfUpdateException ex,
			RedirectAttributes ra) {

		Optional.ofNullable(ex.getForm())
				.map(CourseSelfUpdateDto::id)
				.ifPresent(id ->
						ra.addFlashAttribute(
								"courseId",
								id));

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid input."));

		return "redirect:/courses";
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public String handleValidation(
			ConstraintViolationException ex,
			RedirectAttributes ra) {

		if (ex instanceof ContextConstraintViolationException ctx) {
			ctx.entityId()
					.ifPresent(id ->
							ra.addFlashAttribute(
									"courseId",
									id));
		}

		ra.addFlashAttribute(
				"err",
				messageReader.formatViolations(ex));

		return "redirect:/courses";
	}

	@ExceptionHandler({
			IllegalArgumentException.class,
			IllegalStateException.class
	})
	public String handleBadRequest(
			RuntimeException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid input."));

		return "redirect:/courses";
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public String handleNotFound(
			EntityNotFoundException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Course not found."));

		return "redirect:/courses";
	}

	@ExceptionHandler(AccessDeniedException.class)
	public String handleAccessDenied(
			AccessDeniedException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"err",
				"Access denied.");

		return "redirect:/courses";
	}

	@ExceptionHandler(Exception.class)
	public String handleOther(
			Exception ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"err",
				"Something went wrong.");

		return "redirect:/courses";
	}

	private String buildTeacherReassignmentMessage(
			CourseTeacherReassignmentException ex) {

		var teacher =
				ex.targetTeacher();

		return "Teacher change was not applied. "
				+ "%s %s (%s) has schedule conflicts with %d future course lesson(s)."
						.formatted(
								teacher.firstName(),
								teacher.lastName(),
								teacher.email(),
								ex.conflicts().size());
	}

	private String redirectToCoursesPage(
			HttpServletRequest request) {

		var pageNumber =
				Optional.ofNullable(
						request.getParameter("page"))
						.map(Integer::parseInt)
						.map(page ->
								Math.max(
										page,
										0))
						.orElse(0);

		return "redirect:/courses?page="
				+ pageNumber;
	}
}