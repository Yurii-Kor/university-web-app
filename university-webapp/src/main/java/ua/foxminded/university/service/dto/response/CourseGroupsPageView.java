package ua.foxminded.university.service.dto.response;

import java.util.List;
import ua.foxminded.university.model.repository.dto.CourseHeaderView;
import ua.foxminded.university.model.repository.dto.GroupOptionView;

public record CourseGroupsPageView(
        CourseHeaderView course,
        List<GroupOptionView> assigned,
        List<GroupOptionView> available
) {}