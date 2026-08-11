package ua.foxminded.university.service.studygroup.exception;

import lombok.Getter;

@Getter
public class StudyGroupDeleteException extends RuntimeException {

    private final long groupId;
    private final long studentCount;

    public StudyGroupDeleteException(
            long groupId,
            long studentCount,
            String message) {

        super(message);
        this.groupId = groupId;
        this.studentCount = studentCount;
    }
}