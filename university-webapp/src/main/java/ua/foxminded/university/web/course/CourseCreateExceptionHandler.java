package ua.foxminded.university.web.course;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ua.foxminded.university.service.dto.request.course.CourseCreateDto;

import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice(assignableTypes = CourseCreateController.class)
@RequiredArgsConstructor
public class CourseCreateExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException ex,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("form", formFromRequest(req));
        ra.addFlashAttribute("err", safeMessage(ex, "Invalid request."));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("form", formFromRequest(req));
        ra.addFlashAttribute("err", formatViolations(ex));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex,
                                 HttpServletRequest req,
                                 RedirectAttributes ra) {

        ra.addFlashAttribute("form", formFromRequest(req));
        ra.addFlashAttribute("err", safeMessage(ex, "Teacher not found."));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDbConflict(DataIntegrityViolationException ex,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("form", formFromRequest(req));
        ra.addFlashAttribute("err", "Course code or name already exists.");
        return "redirect:/courses/create";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex,
                              HttpServletRequest req,
                              RedirectAttributes ra) {

        ra.addFlashAttribute("form", formFromRequest(req));
        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/courses/create";
    }

    private CourseCreateDto formFromRequest(HttpServletRequest req) {
        String code = req.getParameter("code");
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        Long teacherId = parseLong(req.getParameter("teacherId"));
        return new CourseCreateDto(code, name, description, teacherId);
    }

    private Long parseLong(String raw) {
        try {
            return Optional.ofNullable(raw)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
    }

    private String safeMessage(Exception ex, String fallback) {
        return Optional.ofNullable(ex.getMessage())
                .map(String::trim)
                .filter(m -> !m.isBlank())
                .orElse(fallback);
    }
}
