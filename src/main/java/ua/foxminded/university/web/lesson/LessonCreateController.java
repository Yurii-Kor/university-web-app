package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/lessons")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class LessonCreateController {

	private final LessonService lessonService;
	private final LessonCreatePageDataLoader pageDataLoader;
	private final PrincipalHandler principalHandler;
	private final Clock clock;

	@InitBinder("form")
	void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(
				String.class,
				new StringTrimmerEditor(true));
	}

	@GetMapping("/create")
	public String createLessonPage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(required = false) Long courseId,
			@RequestParam(required = false) Long groupId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Model model) {

		var teacherId =
				principalHandler.parseUserId(
						principal);

		var selectedDate = Optional.ofNullable(date)
				.orElseGet(this::today);

		var form = new LessonCreateDto(
				courseId,
				groupId,
				selectedDate,
				null,
				null,
				null,
				null,
				null);

		model.addAttribute(
				"form",
				form);

		pageDataLoader.addPageData(
				model,
				teacherId,
				form);

		return "lessons/create";
	}

	@PostMapping("/create")
	public String createLesson(
			@AuthenticationPrincipal UserDetails principal,
			@Valid @ModelAttribute("form") LessonCreateDto form,
			RedirectAttributes redirectAttributes) {

		var teacherId =
				principalHandler.parseUserId(
						principal);

		lessonService.create(
				teacherId,
				form);

		redirectAttributes.addFlashAttribute(
				"ok",
				"Lesson created.");

		return "redirect:/schedule/day?date="
				+ form.date();
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}
}