package ua.foxminded.university.service.studygroup.exception;

import lombok.Getter;

@Getter
public class StudyGroupRenameException extends RuntimeException {

    private final long groupId;
    private final String requestedName;

    public StudyGroupRenameException(
            long groupId,
            String requestedName,
            String message) {

        super(message);
        this.groupId = groupId;
        this.requestedName = requestedName;
    }
}