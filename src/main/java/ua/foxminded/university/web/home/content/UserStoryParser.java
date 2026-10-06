package ua.foxminded.university.web.home.content;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.home.content.dto.UserStoryDefinition;

@Component
@RequiredArgsConstructor
public class UserStoryParser {

    private final Validator validator;

    private final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory())
                    .configure(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false);

    public UserStoryDefinition parse(Resource resource) {
        try (var inputStream = resource.getInputStream()) {
            var definition = yamlMapper.readValue(
                    inputStream,
                    UserStoryDefinition.class);

            if (definition == null) {
                throw new IllegalStateException(
                        "Empty user story definition in "
                                + resource.getDescription());
            }

            validate(
                    definition,
                    resource);

            return definition;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to parse user story resource: "
                            + resource.getDescription(),
                    e);
        }
    }

    private void validate(
            UserStoryDefinition definition,
            Resource resource) {

        var violations = validator.validate(definition);

        if (violations.isEmpty()) {
            return;
        }

        var message = violations.stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        throw new IllegalStateException(
                "Invalid user story definition in "
                        + resource.getDescription()
                        + ": "
                        + message);
    }
}