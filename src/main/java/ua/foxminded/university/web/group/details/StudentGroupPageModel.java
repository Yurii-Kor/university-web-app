package ua.foxminded.university.web.group.details;

public record StudentGroupPageModel(
        String pageTitle,
        String pageSubtitle,
        Long groupId,
        String groupName,
        long studentCount
) implements GroupDetailsPageModel {
}