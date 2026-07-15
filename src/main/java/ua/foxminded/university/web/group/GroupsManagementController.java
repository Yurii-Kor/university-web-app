package ua.foxminded.university.web.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.service.studygroup.StudyGroupService;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupsManagementController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final StudyGroupService studyGroupService;

    @GetMapping
    public String getGroupsPage(@RequestParam(name = "page", defaultValue = "0") int page,
                                Model model) {

        Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<GroupView> groupsPage = studyGroupService.listGroupCardsForAdmin(pageable);

        model.addAttribute("pageTitle", "Groups");
        model.addAttribute("groups", groupsPage.getContent());
        model.addAttribute("currentPage", groupsPage.getNumber());
        model.addAttribute("totalPages", groupsPage.getTotalPages());
        model.addAttribute("hasPrevious", groupsPage.hasPrevious());
        model.addAttribute("hasNext", groupsPage.hasNext());

        return "groups/groups";
    }
}