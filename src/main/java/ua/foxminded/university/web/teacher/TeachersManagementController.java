package ua.foxminded.university.web.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherCardView;
import ua.foxminded.university.service.teacher.TeacherService;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeachersManagementController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TeacherService teacherService;

    @GetMapping
    public String getTeachersPage(@RequestParam(name = "page", defaultValue = "0") int page,
                                  Model model) {

        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<TeacherCardView> teachersPage = teacherService.listTeacherCardsForAdmin(pageable);

        model.addAttribute("pageTitle", "Teachers");
        model.addAttribute("teachers", teachersPage.getContent());
        model.addAttribute("currentPage", teachersPage.getNumber());
        model.addAttribute("totalPages", teachersPage.getTotalPages());
        model.addAttribute("hasPrevious", teachersPage.hasPrevious());
        model.addAttribute("hasNext", teachersPage.hasNext());

        return "teachers/teachers";
    }
}