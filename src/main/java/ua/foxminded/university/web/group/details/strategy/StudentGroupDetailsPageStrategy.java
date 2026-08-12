package ua.foxminded.university.web.group.details.strategy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.group.details.GroupDetailsPageMode;
import ua.foxminded.university.web.group.details.GroupDetailsPageStrategy;
import ua.foxminded.university.web.group.details.StudentGroupPageModel;

@Component
@RequiredArgsConstructor
public class StudentGroupDetailsPageStrategy
        implements GroupDetailsPageStrategy {

    private final StudyGroupService groupService;

    @Override
    public GroupDetailsPageMode mode() {
        return GroupDetailsPageMode.STUDENT;
    }

    @Override
    public StudentGroupPageModel buildPage(
            long userId) {

        var group = groupService.getGroupHeaderForStudent(
                userId);

        return new StudentGroupPageModel(
                mode().getPageTitle(),
                mode().getPageSubtitle(),
                group.id(),
                group.name(),
                group.studentCount());
    }
}