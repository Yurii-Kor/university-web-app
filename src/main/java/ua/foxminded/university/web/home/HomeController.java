package ua.foxminded.university.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.home.page.HomePageModelFactory;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomePageModelFactory pageModelFactory;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute(
                "page",
                pageModelFactory.create());

        return "home";
    }
}