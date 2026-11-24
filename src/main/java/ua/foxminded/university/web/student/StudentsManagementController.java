package ua.foxminded.university.web.student;

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
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.student.dto.StudentUpdateDto;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.student.cards.StudentsCardsPageModelFactory;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentsManagementController {

    private final StudentService studentService;
    private final StudyGroupService studyGroupService;
    private final StudentsCardsPageModelFactory pageFactory;

    @GetMapping
    public String studentsPage(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(name = "page", defaultValue = "0") int pageNumber,
            Model model) {

        var page = pageFactory.build(principal, pageNumber);

        model.addAttribute("page", page);
        model.addAttribute("all_groups", studyGroupService.listGroupOptions());

        return "students/students";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateStudent(
            @PathVariable("id") long id,
            @Valid @ModelAttribute("patch") StudentUpdateDto patch,
            BindingResult br,
            @RequestParam(name = "page", defaultValue = "0") int pageNumber,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            addUpdateValidationErrors(id, patch, br, ra);

            return redirectToStudents(pageNumber);
        }

        studentService.update(
                id,
                patch);

        ra.addFlashAttribute(
                "ok",
                "Student updated.");

        ra.addFlashAttribute(
                "studentId",
                id);

        return redirectToStudents(pageNumber);
    }

    private String redirectToStudents(int pageNumber) {
        return "redirect:/students?page=" + normalizePage(pageNumber);
    }

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }

    private void addUpdateValidationErrors(
            long id,
            StudentUpdateDto patch,
            BindingResult br,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "studentId",
                id);

        ra.addFlashAttribute(
                "requestedGroupId",
                patch.groupId());

        ra.addFlashAttribute(
                "requestedEnrollmentYear",
                patch.enrollmentYear());

        ra.addFlashAttribute(
                "err",
                Optional.ofNullable(br.getFieldError())
                        .map(error -> error.getDefaultMessage())
                        .orElse("Invalid student data."));
    }
}