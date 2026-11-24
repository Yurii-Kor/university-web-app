package ua.foxminded.university.service.lesson.exception;

import java.util.List;

import lombok.Getter;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.lesson.dto.RequestedSlot;

@Getter
public class ScheduleConflictException extends RuntimeException {

	private static final String ERROR_CODE =
			"SCHEDULE_CONFLICT";

	private final Long lessonId;
	private final RequestedSlot requested;
	private final List<OverlapHit> groupConflicts;
	private final List<OverlapHit> teacherConflicts;
	private final List<OverlapHit> roomConflicts;

	public ScheduleConflictException(
			RequestedSlot requested,
			List<OverlapHit> groupConflicts,
			List<OverlapHit> teacherConflicts,
			List<OverlapHit> roomConflicts) {

		this(
				null,
				requested,
				groupConflicts,
				teacherConflicts,
				roomConflicts);
	}

	public ScheduleConflictException(
			Long lessonId,
			RequestedSlot requested,
			List<OverlapHit> groupConflicts,
			List<OverlapHit> teacherConflicts,
			List<OverlapHit> roomConflicts) {

		super(buildMessage(
				requested,
				groupConflicts,
				teacherConflicts,
				roomConflicts));

		this.lessonId = lessonId;
		this.requested = requested;
		this.groupConflicts =
				List.copyOf(groupConflicts);
		this.teacherConflicts =
				List.copyOf(teacherConflicts);
		this.roomConflicts =
				List.copyOf(roomConflicts);
	}

	public String getErrorCode() {
		return ERROR_CODE;
	}

	private static String buildMessage(
			RequestedSlot requested,
			List<OverlapHit> groupConflicts,
			List<OverlapHit> teacherConflicts,
			List<OverlapHit> roomConflicts) {

		return "Schedule conflicts: groups=%d, teachers=%d, rooms=%d "
				+ "[requested: groupId=%d, courseId=%d, teacherId=%d, %s–%s, room=%s]"
						.formatted(
								groupConflicts.size(),
								teacherConflicts.size(),
								roomConflicts.size(),
								requested.groupId(),
								requested.courseId(),
								requested.teacherId(),
								requested.startTime(),
								requested.endTime(),
								requested.room());
	}
}