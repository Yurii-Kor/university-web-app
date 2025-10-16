package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;

import java.util.Collection;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
	
	@Query("select s.id from Student s where s.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Student s set s.group = :newGroup where s.id in :ids")
    int updateGroupByIds(@Param("newGroup") StudyGroup newGroup,
                         @Param("ids") Collection<Long> ids);
}
