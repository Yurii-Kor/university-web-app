package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ua.foxminded.university.model.domain.validation.OnCreate;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "courses")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@NotBlank
	@Size(min = 4, max = 32)
	@Pattern(
			regexp = "^(?:[A-Z]{2,5}-[A-Z]{2,9}-\\d{3}|[A-Z]{2,5}-\\d{3})$",
		    message = "Code must look like 'CSE-ALG-101' or 'SEC-303'"
			)
	@Column(nullable = false, unique = true, length = 32)
	@ToString.Include
	private String code;

	@NotBlank
	@Size(max = 255)
	@Column(nullable = false, unique = true, length = 255)
	@ToString.Include
	private String name;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "teacher_id")
	private Teacher teacher;

	@Builder.Default
	@ManyToMany
	@JoinTable(
			name = "group_courses", 
			joinColumns = @JoinColumn(name = "course_id"), 
			inverseJoinColumns = @JoinColumn(name = "group_id")
			)
	private Set<StudyGroup> groups = new LinkedHashSet<>();
	
	@PrePersist	@PreUpdate
	void normalizeCode() {
	    if (code != null) code = code.trim().toUpperCase();
	}
	
	@AssertTrue(message = "Course.teacher.id must not be null", groups = OnCreate.class)
    private boolean hasTeacherIdOnCreate() {
        return teacher != null && teacher.getId() != null;
    }
	
	@AssertTrue(message = "Course.groups[].id must not be null", groups = OnCreate.class)
	private boolean hasGroupIdsOnCreate() {
	    return groups == null
	        || groups.isEmpty()
	        || groups.stream().noneMatch(g -> g == null || g.getId() == null);
	}
}
