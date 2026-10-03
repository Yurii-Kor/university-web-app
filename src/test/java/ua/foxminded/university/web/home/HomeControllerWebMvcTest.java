package ua.foxminded.university.web.home;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;

import ua.foxminded.university.web.home.page.HomePageModel;
import ua.foxminded.university.web.home.page.HomePageModelFactory;
import ua.foxminded.university.web.home.page.UserStoryFeatureView;
import ua.foxminded.university.web.home.page.UserStoryRoleView;

@WebMvcTest(
        controllers = HomeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = ControllerAdvice.class))
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomePageModelFactory pageModelFactory;

    @Test
    void home_rendersPage() throws Exception {
        var role = new UserStoryRoleView(
                "Student",
                true,
                "Test scope.",
                null,
                List.of("Use the test feature."));

        var feature = new UserStoryFeatureView(
                "test-feature",
                1,
                "Test feature",
                "Test feature description.",
                List.of("Use the common capability."),
                List.of(role));

        var page = new HomePageModel(
                "Home",
                "Test heading",
                "Test page description.",
                List.of(feature));

        when(pageModelFactory.create())
                .thenReturn(page);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute(
                        "page",
                        page))
                .andExpect(content().string(
                        containsString("Test heading")))
                .andExpect(content().string(
                        containsString("Test feature")))
                .andExpect(content().string(
                        containsString("Use the test feature.")));
    }
}