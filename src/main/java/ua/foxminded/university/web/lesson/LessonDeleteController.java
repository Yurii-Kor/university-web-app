package ua.foxminded.university.web.lesson;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/lessons")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class LessonDeleteController {

    private final LessonService lessonService;
    private final PrincipalHandler principalHandler;

    @PostMapping("/{lessonId}/delete")
    public String deleteLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable long lessonId,
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            RedirectAttributes redirectAttributes) {

        var teacherId =
                principalHandler.parseUserId(
                        principal);

        lessonService.deleteById(
                teacherId,
                lessonId);

        redirectAttributes.addFlashAttribute(
                "ok",
                "Lesson deleted.");

        return "redirect:/schedule/day?date="
                + date;
    }
}