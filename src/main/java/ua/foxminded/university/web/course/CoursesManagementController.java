package ua.foxminded.university.web.course;

import java.util.Optional;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.service.course.dto.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.web.bind.TrimToNullUppercaseEditor;
import ua.foxminded.university.web.course.page.CoursesPageModelFactory;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesManagementController {

	private final CourseService courseService;
	private final CoursesPageModelFactory pageFactory;

	@GetMapping
	public String coursesPage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			Model model) {

		var page = pageFactory.build(
				principal,
				pageNumber);

		model.addAttribute(
				"page",
				page);

		return "courses/courses";
	}

	@InitBinder
	void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(
				String.class,
				new StringTrimmerEditor(true));

		binder.registerCustomEditor(
				String.class,
				"description",
				new StringTrimmerEditor(false));

		binder.registerCustomEditor(
				String.class,
				"code",
				new TrimToNullUppercaseEditor());
	}

	@PostMapping("/description/update")
	@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
	public String updateDescription(
			@Valid @ModelAttribute("patch") CourseDescriptionUpdateDto patch,
			BindingResult br,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		if (br.hasErrors()) {
			addDescriptionValidationErrors(
					patch,
					br,
					ra);

			return redirectToCourses(
					pageNumber);
		}

		courseService.updateDescription(
				patch);

		ra.addFlashAttribute(
				"ok",
				"Description updated.");

		ra.addFlashAttribute(
				"courseId",
				patch.id());

		return redirectToCourses(
				pageNumber);
	}

	@PostMapping("/self/update")
	@PreAuthorize("hasRole('ADMIN')")
	public String updateSelf(
			@Valid @ModelAttribute("patch") CourseSelfUpdateDto patch,
			BindingResult br,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		if (br.hasErrors()) {
			addSelfUpdateValidationErrors(
					patch,
					br,
					ra);

			return redirectToCourses(
					pageNumber);
		}

		courseService.updateSelf(
				patch);

		ra.addFlashAttribute(
				"ok",
				"Course updated.");

		ra.addFlashAttribute(
				"courseId",
				patch.id());

		return redirectToCourses(
				pageNumber);
	}

	@PostMapping("/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public String deleteCourse(
			@PathVariable("id") Long id,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		courseService.delete(
				id);

		ra.addFlashAttribute(
				"ok",
				"Course deleted.");

		return redirectToCourses(
				pageNumber);
	}

	private void addDescriptionValidationErrors(
			CourseDescriptionUpdateDto patch,
			BindingResult br,
			RedirectAttributes ra) {

		addCourseTarget(
				patch.id(),
				ra);

		ra.addFlashAttribute(
				"requestedDescription",
				patch.description());

		addValidationMessage(
				br,
				ra);
	}

	private void addSelfUpdateValidationErrors(
			CourseSelfUpdateDto patch,
			BindingResult br,
			RedirectAttributes ra) {

		addCourseTarget(
				patch.id(),
				ra);

		ra.addFlashAttribute(
				"requestedCode",
				patch.code());

		ra.addFlashAttribute(
				"requestedName",
				patch.name());

		ra.addFlashAttribute(
				"requestedTeacherId",
				patch.teacherId());

		addValidationMessage(
				br,
				ra);
	}

	private void addCourseTarget(
			Long courseId,
			RedirectAttributes ra) {

		Optional.ofNullable(courseId)
				.ifPresent(id ->
						ra.addFlashAttribute(
								"courseId",
								id));
	}

	private void addValidationMessage(
			BindingResult br,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"err",
				Optional.ofNullable(
						br.getFieldError())
						.map(error ->
								error.getDefaultMessage())
						.orElse(
								"Invalid course data."));
	}

	private String redirectToCourses(
			int pageNumber) {

		return "redirect:/courses?page="
				+ normalizePage(pageNumber);
	}

	private int normalizePage(
			int pageNumber) {

		return Math.max(
				pageNumber,
				0);
	}
}