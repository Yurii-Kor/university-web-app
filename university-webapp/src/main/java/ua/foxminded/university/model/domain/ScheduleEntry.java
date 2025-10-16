package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.domain.validation.OnCreate;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "schedule_entries")
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull
    @Column(name = "start_time", nullable = false)
    @ToString.Include
    private OffsetDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    @ToString.Include
    private OffsetDateTime endTime;

    @NotBlank
    @Size(max = 64)
    @Pattern(
    		regexp = "^[A-Z]-\\d{3}$", 
    		message = "Room must look like 'B-105'"
    		)
    @Column(name = "room", nullable = false, length = 64)
    @ToString.Include
    private String room;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 32)
    private LessonType lessonType;

    @Column(name = "lesson_description", columnDefinition = "text")
    private String description;

    @AssertTrue(message = "endTime must be after startTime")
    private boolean isTimeRangeValid() {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }
    
    @AssertTrue(message = "schedule.group.id must not be null", groups = OnCreate.class)
    private boolean hasGroupIdOnCreate() {
        return group != null && group.getId() != null;
    }

    @AssertTrue(message = "schedule.course.id must not be null", groups = OnCreate.class)
    private boolean hasCourseIdOnCreate() {
        return course != null && course.getId() != null;
    }
}
