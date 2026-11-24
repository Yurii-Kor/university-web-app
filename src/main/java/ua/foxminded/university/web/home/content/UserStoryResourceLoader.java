package ua.foxminded.university.web.home.content;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class UserStoryResourceLoader {

    private static final String RESOURCES_PATTERN =
            "classpath*:content/user-stories/*.yml";

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public List<Resource> load() {
        try {
            return List.copyOf(
                    Arrays.asList(
                            resolver.getResources(RESOURCES_PATTERN)));

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load user story resources.",
                    e);
        }
    }
}