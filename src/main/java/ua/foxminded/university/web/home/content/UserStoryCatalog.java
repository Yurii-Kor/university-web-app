package ua.foxminded.university.web.home.content;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import ua.foxminded.university.web.home.page.UserStoryFeatureView;

@Component
public class UserStoryCatalog {

    private final List<UserStoryFeatureView> features;

    public UserStoryCatalog(
            UserStoryResourceLoader resourceLoader,
            UserStoryParser parser) {

        this.features =
                resourceLoader.load()
                        .stream()
                        .map(parser::parse)
                        .sorted(
                                Comparator.comparingInt(
                                        UserStoryFeatureView::order))
                        .toList();
    }

    public List<UserStoryFeatureView> getFeatures() {
        return features;
    }
}