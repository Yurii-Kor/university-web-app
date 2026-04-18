package ua.foxminded.university.web.account;

import java.util.List;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.AppUserRoleTransitionService;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;

@Controller
@RequestMapping("/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountManagementController {

    private final AccountsPageModelFactory pageFactory;
    private final StudyGroupService studyGroupService;

    private final AppUserRoleTransitionService roleTransitionService;
    private final AppUserService appUserService;
    private final StudentService studentService;
    private final TeacherService teacherService;

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String accountsPage(@RequestParam(name = "view", defaultValue = "students") String view,
                               @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                               Model model) {

        var page = pageFactory.build(view, pageNumber);

        model.addAttribute("page", page);
        model.addAttribute("all_groups", studyGroupService.listGroupOptions());
        model.addAttribute("all_ranks", AcademicRank.values());

        return "accounts/accounts";
    }

    @PostMapping("/students/to-teacher")
    public String changeStudentToTeacher(@ModelAttribute StudentToTeacherRoleChangeDto form,
                                         @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                         RedirectAttributes ra) {

        roleTransitionService.changeStudentToTeacher(form);

        ra.addFlashAttribute("ok", "Student role changed to teacher.");
        ra.addFlashAttribute("accountId", form.userId());

        return "redirect:/accounts?view=students&page=" + normalizePage(pageNumber);
    }

    @PostMapping("/teachers/to-student")
    public String changeTeacherToStudent(@ModelAttribute TeacherToStudentRoleChangeDto form,
                                         @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                         RedirectAttributes ra) {

        roleTransitionService.changeTeacherToStudent(form);

        ra.addFlashAttribute("ok", "Teacher role changed to student.");
        ra.addFlashAttribute("accountId", form.userId());

        return "redirect:/accounts?view=teachers&page=" + normalizePage(pageNumber);
    }

    @PostMapping("/{id}/enable")
    public String enableAccount(@PathVariable("id") long id,
                                @RequestParam(name = "view", defaultValue = "students") String view,
                                @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                RedirectAttributes ra) {

        appUserService.enableUserByIds(id);

        ra.addFlashAttribute("ok", "Account enabled.");
        ra.addFlashAttribute("accountId", id);

        return "redirect:/accounts?view=" + normalizeView(view) + "&page=" + normalizePage(pageNumber);
    }

    @PostMapping("/{id}/disable")
    public String disableAccount(@PathVariable("id") long id,
                                 @RequestParam(name = "view", defaultValue = "students") String view,
                                 @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                 RedirectAttributes ra) {

        appUserService.disableUserByIds(id);

        ra.addFlashAttribute("ok", "Account disabled.");
        ra.addFlashAttribute("accountId", id);

        return "redirect:/accounts?view=" + normalizeView(view) + "&page=" + normalizePage(pageNumber);
    }

    @PostMapping("/{id}/delete")
    public String deleteAccount(@PathVariable("id") long id,
                                @RequestParam(name = "view", defaultValue = "students") String view,
                                @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                RedirectAttributes ra) {

        var safeView = normalizeView(view);

        if ("teachers".equals(safeView)) {
            teacherService.deleteByIds(List.of(id));
            ra.addFlashAttribute("ok", "Teacher account deleted.");
        } else {
            studentService.deleteByIds(List.of(id));
            ra.addFlashAttribute("ok", "Student account deleted.");
        }

        return "redirect:/accounts?view=" + safeView + "&page=" + normalizePage(pageNumber);
    }

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }

    private String normalizeView(String view) {
        return "teachers".equalsIgnoreCase(view) ? "teachers" : "students";
    }
}