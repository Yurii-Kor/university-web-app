package ua.foxminded.university.service.dto.response;

import java.util.List;
import ua.foxminded.university.model.repository.dto.CourseHeaderView;
import ua.foxminded.university.model.repository.dto.GroupView;

public record CourseGroupsView(
        CourseHeaderView course,
        List<GroupView> assigned,
        List<GroupView> available
) {}