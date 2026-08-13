package ua.foxminded.university.web.group;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;

@Controller
@RequestMapping("/groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GroupCreateController {

    private final StudyGroupService groupService;

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true));
    }

    @GetMapping("/create")
    public String createGroupPage(
            @ModelAttribute("form") StudyGroupCreateDto form) {

        return "groups/create";
    }

    @PostMapping("/create")
    public String createGroup(
            @Valid @ModelAttribute("form") StudyGroupCreateDto form,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            return "groups/create";
        }

        groupService.create(form);

        ra.addFlashAttribute(
                "ok",
                "Group created.");

        return "redirect:/groups";
    }
}