package ua.foxminded.university.web.home.content;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import ua.foxminded.university.web.home.content.dto.UserStoryDefinition;

@Component
public class UserStoryCatalog {

    private final List<UserStoryDefinition> definitions;

    public UserStoryCatalog(
            UserStoryResourceLoader resourceLoader,
            UserStoryParser parser) {

        this.definitions =
                resourceLoader.load()
                        .stream()
                        .map(parser::parse)
                        .sorted(
                                Comparator.comparingInt(
                                        UserStoryDefinition::order))
                        .toList();
    }

    public List<UserStoryDefinition> getDefinitions() {
        return definitions;
    }
}