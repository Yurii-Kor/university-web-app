package ua.foxminded.university.web.group.details;

public interface GroupDetailsPageStrategy {

    GroupDetailsPageMode mode();

    GroupDetailsPageModel buildPage(long userId);
}