package ua.foxminded.university.service.lesson.dto;

import java.util.List;

import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;

public record LessonConflictResult(
        List<OverlapHit> groupConflicts,
        List<OverlapHit> teacherConflicts,
        List<OverlapHit> roomConflicts
) {

    public boolean hasConflicts() {
        return !groupConflicts.isEmpty()
                || !teacherConflicts.isEmpty()
                || !roomConflicts.isEmpty();
    }
}