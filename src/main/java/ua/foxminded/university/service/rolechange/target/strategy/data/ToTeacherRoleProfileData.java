package ua.foxminded.university.service.rolechange.target.strategy.data;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

public interface ToTeacherRoleProfileData extends TargetRoleProfileData {

    AcademicRank academicRank();

    String office();
}