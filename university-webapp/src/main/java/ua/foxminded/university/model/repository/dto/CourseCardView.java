package ua.foxminded.university.model.repository.dto;

public record CourseCardView(
	    Long id,
	    String code,
	    String name,
	    String description,
	    Long teacherId,
	    String teacherEmail,
	    String teacherFirstName,
	    String teacherLastName,
	    long groupsCount
	) {}
