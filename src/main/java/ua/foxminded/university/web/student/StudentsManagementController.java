package ua.foxminded.university.web.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.service.student.StudentService;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentsManagementController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final StudentService studentService;

    @GetMapping
    public String getStudentsPage(@RequestParam(name = "page", defaultValue = "0") int page,
                                  Model model) {

        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<StudentCardView> studentsPage = studentService.listStudentCardsForAdmin(pageable);

        model.addAttribute("pageTitle", "Students");
        model.addAttribute("students", studentsPage.getContent());
        model.addAttribute("currentPage", studentsPage.getNumber());
        model.addAttribute("totalPages", studentsPage.getTotalPages());
        model.addAttribute("hasPrevious", studentsPage.hasPrevious());
        model.addAttribute("hasNext", studentsPage.hasNext());

        return "students/students";
    }
}