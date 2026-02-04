package ua.foxminded.university.web.view;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PrincipalHandler principalHandler;
    
    @GetMapping("/admin")
    public String admin(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/admin";
    }


    @GetMapping("/courses")
    public String courses(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/courses";
    }

    @GetMapping("/groups")
    public String groups(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/groups";
    }

    @GetMapping("/students")
    public String students(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/students";
    }

    @GetMapping("/teachers")
    public String teachers(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/teachers";
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal UserDetails principal) {
        principalHandler.requirePrincipal(principal);
        return "views/schedule";
    }
}
