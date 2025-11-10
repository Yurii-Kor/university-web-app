package ua.foxminded.university.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	Optional<AppUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	long countByRole(UserRole role);

	long countByRoleAndEnabled(UserRole role, boolean enabled);
	
	@Query("""
			select lower(u.email)
			from AppUser u
			where lower(u.email) in :emails
			""")
	List<String> findExistingEmailsIgnoreCase(@Param("emails") Collection<String> emails);

	@Query("select u.id from AppUser u where u.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Query("select u.id from AppUser u where u.id in :ids and u.role = :role and u.enabled = true")
	List<Long> findEnabledIdsByRoleIn(@Param("ids") Collection<Long> ids, @Param("role") UserRole role);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update AppUser u set u.enabled = :enabled where u.id in :ids")
	int setEnabledForIds(@Param("ids") Collection<Long> ids, @Param("enabled") boolean enabled);
}
