package ua.foxminded.university.web.home.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ua.foxminded.university.web.home.page.UserStoryFeatureView;
import ua.foxminded.university.web.home.page.UserStoryRoleView;

@Component
public class UserStoryParser {

    private static final List<String> ROLE_ORDER =
            List.of("STUDENT", "TEACHER", "ADMIN");

    private final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory());

    public UserStoryFeatureView parse(Resource resource) {
        try (var inputStream = resource.getInputStream()) {
            var root = yamlMapper.readTree(inputStream);

            var id = requiredText(root, "id", resource);
            var title = requiredText(root, "title", resource);
            var order = requiredInt(root, "order", resource);
            var description =
                    requiredText(root, "description", resource);

            var commonStories =
                    readStrings(
                            root.path("common")
                                    .path("stories"));

            var roles =
                    readRoles(
                            root.path("roles"),
                            resource);

            return new UserStoryFeatureView(
                    id,
                    order,
                    title,
                    description,
                    commonStories,
                    roles);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to parse user story resource: "
                            + resource.getDescription(),
                    e);
        }
    }

    private List<UserStoryRoleView> readRoles(
            JsonNode rolesNode,
            Resource resource) {

        var roles = new ArrayList<UserStoryRoleView>();

        for (var roleName : ROLE_ORDER) {
            var roleNode = rolesNode.path(roleName);

            if (!roleNode.isObject()) {
                throw new IllegalStateException(
                        "Missing role "
                                + roleName
                                + " in "
                                + resource.getDescription());
            }

            roles.add(
                    new UserStoryRoleView(
                            toDisplayName(roleName),
                            roleNode.path("available")
                                    .asBoolean(false),
                            optionalText(
                                    roleNode,
                                    "scope"),
                            optionalText(
                                    roleNode,
                                    "reason"),
                            readStrings(
                                    roleNode.path("stories"))));
        }

        return List.copyOf(roles);
    }

    private List<String> readStrings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }

        var values = new ArrayList<String>();

        node.forEach(value ->
                values.add(value.asText()));

        return List.copyOf(values);
    }

    private String requiredText(
            JsonNode root,
            String field,
            Resource resource) {

        var value = root.path(field).asText();

        if (value.isBlank()) {
            throw new IllegalStateException(
                    "Missing field '"
                            + field
                            + "' in "
                            + resource.getDescription());
        }

        return value;
    }

    private int requiredInt(
            JsonNode root,
            String field,
            Resource resource) {

        var node = root.path(field);

        if (!node.isInt()) {
            throw new IllegalStateException(
                    "Missing or invalid field '"
                            + field
                            + "' in "
                            + resource.getDescription());
        }

        return node.asInt();
    }

    private String optionalText(
            JsonNode node,
            String field) {

        var value = node.path(field);

        return value.isMissingNode()
                || value.isNull()
                || value.asText().isBlank()
                        ? null
                        : value.asText();
    }

    private String toDisplayName(String roleName) {
        return roleName.charAt(0)
                + roleName.substring(1)
                        .toLowerCase();
    }
}