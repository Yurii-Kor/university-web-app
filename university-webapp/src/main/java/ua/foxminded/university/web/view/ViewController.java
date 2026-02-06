package ua.foxminded.university.web.view;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {   

    @GetMapping("/courses")
    public String courses() {
        return "views/courses";
    }

    @GetMapping("/groups")
    public String groups() {
        return "views/groups";
    }

    @GetMapping("/students")
    public String students() {
        return "views/students";
    }

    @GetMapping("/teachers")
    public String teachers() {
        return "views/teachers";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "views/schedule";
    }
}
