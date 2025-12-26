package ua.foxminded.university.service.exception;

import java.util.List;
import ua.foxminded.university.model.repository.dto.OverlapHit;
import ua.foxminded.university.service.exception.dto.RequestedSlot;

public class ScheduleConflictException extends RuntimeException {
    private final RequestedSlot requested;
    private final List<OverlapHit> groupConflicts;
    private final List<OverlapHit> teacherConflicts;
    private final List<OverlapHit> roomConflicts;
    private final String errorCode = "SCHEDULE_CONFLICT";

    public ScheduleConflictException(RequestedSlot requested,
                                     List<OverlapHit> groupConflicts,
                                     List<OverlapHit> teacherConflicts,
                                     List<OverlapHit> roomConflicts) {
        super(buildMessage(requested, groupConflicts, teacherConflicts, roomConflicts));
        this.requested = requested;
        this.groupConflicts = List.copyOf(groupConflicts);
        this.teacherConflicts = List.copyOf(teacherConflicts);
        this.roomConflicts = List.copyOf(roomConflicts);
    }

    public RequestedSlot requested()                 { return requested; }
    public List<OverlapHit> groupConflicts()         { return groupConflicts; }
    public List<OverlapHit> teacherConflicts()       { return teacherConflicts; }
    public List<OverlapHit> roomConflicts()          { return roomConflicts; }
    public String errorCode()                        { return errorCode; }

    private static String buildMessage(RequestedSlot r,
                                       List<OverlapHit> g, List<OverlapHit> t, List<OverlapHit> rm) {
        return "Schedule conflicts: groups=" + g.size() +
               ", teachers=" + t.size() +
               ", rooms=" + rm.size() +
               " [requested: groupId=" + r.groupId() +
               ", courseId=" + r.courseId() +
               ", teacherId=" + r.teacherId() +
               ", " + r.startTime() + "–" + r.endTime() +
               ", room=" + r.room() + "]";
    }
}
