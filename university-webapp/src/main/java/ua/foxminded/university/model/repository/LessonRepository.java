package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.repository.dto.OverlapHit;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllByGroup_IdAndStartTimeBetweenOrderByStartTime(
            Long groupId, OffsetDateTime from, OffsetDateTime to);

    List<Lesson> findAllByCourse_IdAndStartTimeBetweenOrderByStartTime(
            Long courseId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select l
            from Lesson l
            join l.course c
            join c.teacher t
            where t.id = :teacherId
              and l.startTime between :from and :to
            order by l.startTime
            """)
    List<Lesson> findTeacherSchedule(@Param("teacherId") Long teacherId,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to);

    @Query("""
            select new ua.foxminded.university.model.repository.dto.OverlapHit(
                l.id, l.group.id, l.course.id, l.course.teacher.id, l.startTime, l.endTime, l.room
            )
            from Lesson l
            where l.group.id = :groupId
              and l.startTime < :end
              and l.endTime   > :start
            """)
    List<OverlapHit> findGroupOverlaps(@Param("groupId") Long groupId,
                                       @Param("start") OffsetDateTime start,
                                       @Param("end") OffsetDateTime end);

    @Query("""
            select new ua.foxminded.university.model.repository.dto.OverlapHit(
                l.id, l.group.id, l.course.id, c.teacher.id, l.startTime, l.endTime, l.room
            )
            from Lesson l
            join l.course c
            where c.teacher.id = :teacherId
              and l.startTime < :end
              and l.endTime   > :start
            """)
    List<OverlapHit> findTeacherOverlaps(@Param("teacherId") Long teacherId,
                                         @Param("start") OffsetDateTime start,
                                         @Param("end") OffsetDateTime end);

    @Query("""
            select new ua.foxminded.university.model.repository.dto.OverlapHit(
                l.id, l.group.id, l.course.id, l.course.teacher.id, l.startTime, l.endTime, l.room
            )
            from Lesson l
            where upper(l.room) = upper(:room)
              and l.startTime < :end
              and l.endTime   > :start
            """)
    List<OverlapHit> findRoomOverlaps(@Param("room") String room,
                                      @Param("start") OffsetDateTime start,
                                      @Param("end") OffsetDateTime end);

    @Query("select l.id from Lesson l where l.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

    @Query("""
            select l.id
            from Lesson l
            where l.id in :ids and l.course.teacher.id = :teacherId
            """)
    List<Long> findOwnedIds(@Param("ids") Collection<Long> ids,
                            @Param("teacherId") Long teacherId);
}
