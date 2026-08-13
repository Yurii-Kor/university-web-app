package ua.foxminded.university.service.appuser;

final class AppUserServiceTestConstants {

    static final long TEST_ADMIN_ID = 1001L;
    static final long STUDENT_USER_ID = 1002L;
    static final long MISSING_ID = 999_999L;

    static final String TEST_ADMIN_EMAIL = "admin.test@example.com";
    static final String TAKEN_EMAIL = "student.taken@example.com";
    static final String UPDATED_EMAIL = "admin.updated@example.com";
    static final String ADMIN_CREATE_EMAIL = "admin.create@example.com";
    static final String ADMIN_LIST_EMAIL = "admin.list@example.com";
    static final String ADMIN_UPDATE_EMAIL = "admin.update@example.com";
    static final String ADMIN_UPDATED_EMAIL = "admin.updated@example.com";
    static final String ADMIN_NULL_PASSWORD_EMAIL =
            "admin.null.password@example.com";

    static final String FIRST_NAME = "Alice";
    static final String LAST_NAME = "Cooper";

    static final String UPDATED_FIRST_NAME = "UpdatedFirstName";
    static final String UPDATED_LAST_NAME = "UpdatedLastName";

    static final String PWD = "Abcd1234!";
    static final String PWD_NEW = "Abcd1234!x";
    static final String PWD_BAD = "bad";

    static final int ONE_USER = 1;

    private AppUserServiceTestConstants() {
    }
}