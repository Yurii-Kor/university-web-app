package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Teacher;

import java.util.Collection;
import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

	boolean existsByOfficeIgnoreCase(String office);
	
	@Query("select t.id from Teacher t where t.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
}
