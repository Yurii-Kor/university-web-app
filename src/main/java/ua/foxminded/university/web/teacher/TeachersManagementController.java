package ua.foxminded.university.web.teacher;

import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.service.teacher.dto.TeacherUpdateDto;
import ua.foxminded.university.web.teacher.page.TeachersPageModelFactory;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeachersManagementController {

	private final TeacherService teacherService;
	private final TeachersPageModelFactory pageFactory;

	@GetMapping
	public String teachersPage(
			@AuthenticationPrincipal UserDetails principal,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			Model model) {

		var page = pageFactory.build(
				principal,
				pageNumber);

        model.addAttribute("page", page);

		return "teachers/teachers";
	}

	@PostMapping("/{id}/update")
	@PreAuthorize("hasRole('ADMIN')")
	public String updateTeacher(
			@PathVariable("id") long id,
			@Valid @ModelAttribute("patch") TeacherUpdateDto patch,
			BindingResult br,
			@RequestParam(name = "page", defaultValue = "0") int pageNumber,
			RedirectAttributes ra) {

		if (br.hasErrors()) {
			addUpdateValidationErrors(
					id,
					patch,
					br,
					ra);

            return redirectToTeachers(pageNumber);
		}

        teacherService.update(id, patch);

        ra.addFlashAttribute("ok", "Teacher updated.");
        ra.addFlashAttribute("teacherId", id);

        return redirectToTeachers(pageNumber);
	}

    private String redirectToTeachers(int pageNumber) {

		return "redirect:/teachers?page="
				+ normalizePage(pageNumber);
	}

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }

	private void addUpdateValidationErrors(
			long id,
			TeacherUpdateDto patch,
			BindingResult br,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				"teacherId",
				id);

		ra.addFlashAttribute(
				"requestedAcademicRank",
				patch.academicRank());

		ra.addFlashAttribute(
				"requestedOffice",
				patch.office());

		ra.addFlashAttribute(
				"err",
				Optional.ofNullable(
						br.getFieldError())
						.map(error ->
								error.getDefaultMessage())
						.orElse(
								"Invalid teacher data."));
	}
}