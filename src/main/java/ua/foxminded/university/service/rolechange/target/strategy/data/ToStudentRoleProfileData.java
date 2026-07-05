package ua.foxminded.university.service.rolechange.target.strategy.data;

import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

public interface ToStudentRoleProfileData extends TargetRoleProfileData {

    Long groupId();

    Integer enrollmentYear();
}
