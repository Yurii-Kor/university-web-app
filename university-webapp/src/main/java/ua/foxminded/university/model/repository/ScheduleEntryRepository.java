package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.foxminded.university.model.domain.ScheduleEntry;
import ua.foxminded.university.model.repository.dto.OverlapHit;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

	List<ScheduleEntry> findAllByGroup_IdAndStartTimeBetweenOrderByStartTime(Long groupId, OffsetDateTime from,
			OffsetDateTime to);

	List<ScheduleEntry> findAllByCourse_IdAndStartTimeBetweenOrderByStartTime(Long courseId, OffsetDateTime from,
			OffsetDateTime to);

	@Query("""
			select se
			from ScheduleEntry se
			join se.course c
			join c.teacher t
			where t.id = :teacherId
			  and se.startTime between :from and :to
			order by se.startTime
			""")
	List<ScheduleEntry> findTeacherSchedule(@Param("teacherId") Long teacherId, 
										    @Param("from") OffsetDateTime from,
										    @Param("to") OffsetDateTime to);

	@Query("""
			select new ua.foxminded.university.model.repository.dto.OverlapHit(
			    se.id, se.group.id, se.course.id, se.course.teacher.id, se.startTime, se.endTime, se.room
			)
			from ScheduleEntry se
			where se.group.id = :groupId
			  and se.startTime < :end
			  and se.endTime   > :start
			""")
	List<OverlapHit> findGroupOverlaps(@Param("groupId") Long groupId, 
									   @Param("start") OffsetDateTime start,
									   @Param("end") OffsetDateTime end);

	@Query("""
			select new ua.foxminded.university.model.repository.dto.OverlapHit(
			    se.id, se.group.id, se.course.id, c.teacher.id, se.startTime, se.endTime, se.room
			)
			from ScheduleEntry se
			join se.course c
			where c.teacher.id = :teacherId
			  and se.startTime < :end
			  and se.endTime   > :start
			""")
	List<OverlapHit> findTeacherOverlaps(@Param("teacherId") Long teacherId, 
										 @Param("start") OffsetDateTime start,
										 @Param("end") OffsetDateTime end);

	@Query("""
			select new ua.foxminded.university.model.repository.dto.OverlapHit(
			    se.id, se.group.id, se.course.id, se.course.teacher.id, se.startTime, se.endTime, se.room
			)
			from ScheduleEntry se
			where upper(se.room) = upper(:room)
			  and se.startTime < :end
			  and se.endTime   > :start
			""")
	List<OverlapHit> findRoomOverlaps(@Param("room") String room,
									  @Param("start") OffsetDateTime start,
									  @Param("end") OffsetDateTime end);
	
	
	@Query("select e.id from ScheduleEntry e where e.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Query("""
			select e.id
			from ScheduleEntry e
			where e.id in :ids and e.course.teacher.id = :teacherId
			""")
	List<Long> findOwnedIds(@Param("ids") Collection<Long> ids, 
							@Param("teacherId") Long teacherId);
}
