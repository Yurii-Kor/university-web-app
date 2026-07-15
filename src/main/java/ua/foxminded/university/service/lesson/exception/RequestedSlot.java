package ua.foxminded.university.service.lesson.exception;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.Lesson;

public record RequestedSlot(
     Long groupId,
     Long courseId,
     Long teacherId,
     OffsetDateTime startTime,
     OffsetDateTime endTime,
     String room,
     String lessonType,
     String description
) {
 public static RequestedSlot from(Lesson draft, Long teacherId) {
     return new RequestedSlot(
             draft.getGroup()  != null ? draft.getGroup().getId()  : null,
             draft.getCourse() != null ? draft.getCourse().getId() : null,
             teacherId,
             draft.getStartTime(),
             draft.getEndTime(),
             draft.getRoom() != null ? draft.getRoom().trim().toUpperCase() : null,
             draft.getLessonType() != null ? draft.getLessonType().name() : null,
             draft.getDescription()
     );
 }
}

