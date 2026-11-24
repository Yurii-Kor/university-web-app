package ua.foxminded.university.web.lesson;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/lessons")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class LessonUpdateController {

    private static final String EDIT_VIEW =
            "lessons/edit";

    private final LessonService lessonService;
    private final LessonUpdatePageDataLoader pageDataLoader;
    private final PrincipalHandler principalHandler;

    @InitBinder("form")
    void initBinder(
            WebDataBinder binder) {

        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true));
    }

    @GetMapping("/{lessonId}/edit")
    public String editLessonPage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable long lessonId,
            Model model) {

        var teacherId =
                principalHandler.parseUserId(
                        principal);

        var lesson =
                lessonService.getTeacherScheduleItem(
                        teacherId,
                        lessonId);

        var form =
                pageDataLoader.toForm(
                        lesson);

        model.addAttribute(
                "form",
                form);

        pageDataLoader.addPageData(
                model,
                teacherId,
                lesson,
                form);

        return EDIT_VIEW;
    }

    @PostMapping("/{lessonId}/edit")
    public String updateLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable long lessonId,
            @Valid @ModelAttribute("form") LessonUpdateDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        var teacherId =
                principalHandler.parseUserId(
                        principal);

        if (bindingResult.hasErrors()) {

            pageDataLoader.addPageData(
                    model,
                    teacherId,
                    lessonId,
                    form);

            return EDIT_VIEW;
        }

        lessonService.update(
                teacherId,
                lessonId,
                form);

        redirectAttributes.addFlashAttribute(
                "ok",
                "Lesson updated.");

        return "redirect:/schedule/day?date="
                + form.date();
    }
}