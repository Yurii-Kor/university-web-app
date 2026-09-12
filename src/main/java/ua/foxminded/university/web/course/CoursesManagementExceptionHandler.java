package ua.foxminded.university.web.course;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.service.course.exception.CourseSelfUpdateException;
import ua.foxminded.university.service.course.exception.CourseTeacherReassignmentException;
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
				buildTeacherReassignmentMessage(
						ex));

		ra.addFlashAttribute(
				"teacherReassignmentError",
				ex);

		return redirectToCoursesPage(
				request);
	}

	@ExceptionHandler(CourseSelfUpdateException.class)
	public String handleSelfUpdate(
			CourseSelfUpdateException ex,
			HttpServletRequest request,
			RedirectAttributes ra) {

		Optional.ofNullable(
				ex.getForm())
				.ifPresent(form ->
						addSelfUpdateErrorAttributes(
								form,
								ra));

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid input."));

		return redirectToCoursesPage(
				request);
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

	private void addSelfUpdateErrorAttributes(
			CourseSelfUpdateDto form,
			RedirectAttributes ra) {

		Optional.ofNullable(
				form.id())
				.ifPresent(id ->
						ra.addFlashAttribute(
								"courseId",
								id));

		ra.addFlashAttribute(
				"requestedCode",
				form.code());

		ra.addFlashAttribute(
				"requestedName",
				form.name());

		ra.addFlashAttribute(
				"requestedTeacherId",
				form.teacherId());
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