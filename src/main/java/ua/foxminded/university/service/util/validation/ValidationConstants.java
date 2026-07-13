package ua.foxminded.university.service.util.validation;

public final class ValidationConstants {

	private ValidationConstants() {}
	
	public static final int EMAIL_MAX = 255;

    public static final int PASSWORD_MIN = 8;
    
    public static final int PASSWORD_MAX = 100;

    public static final int PERSON_NAME_MIN = 2;
    
    public static final int PERSON_NAME_MAX = 64;

    public static final int COURSE_CODE_MIN = 4;
    
    public static final int COURSE_CODE_MAX = 32;

    public static final int COURSE_NAME_MAX = 255;

    public static final int DESCRIPTION_MAX = 10_000;

    public static final int GROUP_NAME_MAX = 255;

    public static final int ROOM_MAX = 64;
    
    public static final int ENROLLMENT_YEAR_MIN = 2000;
    
    public static final int ENROLLMENT_YEAR_MAX = 2100;

	public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{Punct}_])(?=\\S+$).{8,100}$";

	public static final String PASSWORD_MESSAGE = "Password must be 8–100 chars, include upper/lowercase, digit, special, and contain no spaces";

	public static final String PERSON_NAME_REGEX = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$";

	public static final String PERSON_NAME_MESSAGE = "Name must contain only letters, optional '-' or ' and no spaces";

	public static final String COURSE_CODE_REGEX = "^(?:[A-Z]{2,5}-[A-Z]{2,9}-\\d{3}|[A-Z]{2,5}-\\d{3})$";

	public static final String COURSE_CODE_MESSAGE = "Code must look like 'CSE-ALG-101' or 'SEC-303'";

	public static final String TEXT_NOT_BLANK_WHEN_PRESENT_REGEX = "^\\s*$|\\S[\\s\\S]*";

	public static final String TEXT_NOT_BLANK_WHEN_PRESENT_MESSAGE = "Name must not be blank when provided";

	public static final String GROUP_NAME_REGEX = "^[A-Z]{2}-\\d{3}$";

	public static final String GROUP_NAME_MESSAGE = "Group name must look like 'CS-101'";

	public static final String ROOM_CODE_REGEX = "^[A-Z]-\\d{3}$";

	public static final String ROOM_MESSAGE = "Room must look like 'B-105'";

	public static final String OFFICE_MESSAGE = "Office must look like 'B-105' (Letter-3digits)";
}
