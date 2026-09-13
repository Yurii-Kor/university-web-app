package ua.foxminded.university.model.persistence.lesson;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	@Query("""
			select l
			from Lesson l
			where l.group.id = :groupId
			  and l.startTime >= :from
			  and l.startTime < :to
			order by l.startTime, l.id
			""")
	List<Lesson> findGroupSchedule(
			@Param("groupId") Long groupId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select l
			from Lesson l
			where l.course.id = :courseId
			  and l.startTime >= :from
			  and l.startTime < :to
			order by l.startTime, l.id
			""")
	List<Lesson> findCourseSchedule(
			@Param("courseId") Long courseId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select l
			from Lesson l
			where l.assignedTeacher.id = :teacherId
			  and l.startTime >= :from
			  and l.startTime < :to
			order by l.startTime, l.id
			""")
	List<Lesson> findTeacherSchedule(
			@Param("teacherId") Long teacherId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select new ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView(
				l.id,
				c.id,
				c.code,
				c.name,
				g.id,
				g.name,
				t.id,
				u.firstName,
				u.lastName,
				l.startTime,
				l.endTime,
				l.room,
				l.lessonType,
				l.description
			)
			from Lesson l
			join l.course c
			join l.group g
			join l.assignedTeacher t
			join t.user u
			where g.id = :groupId
			  and l.startTime >= :from
			  and l.startTime < :to
			order by l.startTime, l.id
			""")
	List<LessonScheduleItemView> findGroupScheduleItems(
			@Param("groupId") Long groupId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select new ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView(
				l.id,
				c.id,
				c.code,
				c.name,
				g.id,
				g.name,
				t.id,
				u.firstName,
				u.lastName,
				l.startTime,
				l.endTime,
				l.room,
				l.lessonType,
				l.description
			)
			from Lesson l
			join l.course c
			join l.group g
			join l.assignedTeacher t
			join t.user u
			where t.id = :teacherId
			  and l.startTime >= :from
			  and l.startTime < :to
			order by l.startTime, l.id
			""")
	List<LessonScheduleItemView> findTeacherScheduleItems(
			@Param("teacherId") Long teacherId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	@Query("""
			select l
			from Lesson l
			where l.course.id = :courseId
			  and l.startTime > :now
			order by l.startTime, l.id
			""")
	List<Lesson> findFutureByCourseId(
			@Param("courseId") Long courseId,
			@Param("now") OffsetDateTime now);

	@Query("""
			select new ua.foxminded.university.model.persistence.lesson.projection.OverlapHit(
				l.id,
				l.group.id,
				l.course.id,
				l.assignedTeacher.id,
				l.startTime,
				l.endTime,
				l.room
			)
			from Lesson l
			where l.group.id = :groupId
			  and l.startTime < :end
			  and l.endTime > :start
			order by l.startTime, l.id
			""")
	List<OverlapHit> findGroupOverlaps(
			@Param("groupId") Long groupId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);

	@Query("""
			select new ua.foxminded.university.model.persistence.lesson.projection.OverlapHit(
				l.id,
				l.group.id,
				l.course.id,
				l.assignedTeacher.id,
				l.startTime,
				l.endTime,
				l.room
			)
			from Lesson l
			where l.assignedTeacher.id = :teacherId
			  and l.startTime < :end
			  and l.endTime > :start
			order by l.startTime, l.id
			""")
	List<OverlapHit> findTeacherOverlaps(
			@Param("teacherId") Long teacherId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);

	@Query("""
			select new ua.foxminded.university.model.persistence.lesson.projection.OverlapHit(
				l.id,
				l.group.id,
				l.course.id,
				l.assignedTeacher.id,
				l.startTime,
				l.endTime,
				l.room
			)
			from Lesson l
			where upper(l.room) = upper(:room)
			  and l.startTime < :end
			  and l.endTime > :start
			order by l.startTime, l.id
			""")
	List<OverlapHit> findRoomOverlaps(
			@Param("room") String room,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);
}