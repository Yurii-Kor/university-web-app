package ua.foxminded.university.web.group;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.group.details.GroupDetailsPageModelFactory;

@Controller
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupDetailsController {

    private final GroupDetailsPageModelFactory pageFactory;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public String groupPage(
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        var result = pageFactory.build(principal);

        model.addAttribute(
                "page",
                result.page());

        return result.viewName();
    }
}