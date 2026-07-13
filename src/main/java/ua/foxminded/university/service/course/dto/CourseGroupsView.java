package ua.foxminded.university.service.course.dto;

import java.util.List;

import ua.foxminded.university.model.persistence.course.projection.CourseHeaderView;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;

public record CourseGroupsView(
        CourseHeaderView course,
        List<GroupView> assigned,
        List<GroupView> available
) {}