package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ua.foxminded.university.model.domain.enums.LessonType;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column()
    @ToString.Include
    private OffsetDateTime startTime;

    @Column()
    @ToString.Include
    private OffsetDateTime endTime;

    @Column()
    @ToString.Include
    private String room;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column()
    private LessonType lessonType;

    @Column()
    private String description;
}
