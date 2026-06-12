package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import lombok.*;

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

	@Column()
	@ToString.Include
	private String code;

	@Column()
	@ToString.Include
	private String name;

	@Column()
	private String description;

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
}
