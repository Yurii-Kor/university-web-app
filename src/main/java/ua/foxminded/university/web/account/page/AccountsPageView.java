package ua.foxminded.university.web.account.page;

public enum AccountsPageView {

    STUDENTS(
            "Account Management",
            "Manage sensitive operations for student accounts.",
            "students"
    ),
    TEACHERS(
            "Account Management",
            "Manage sensitive operations for teacher accounts.",
            "teachers"
    );

    private final String pageTitle;
    private final String pageSubtitle;
    private final String currentView;

    AccountsPageView(String pageTitle, String pageSubtitle, String currentView) {
        this.pageTitle = pageTitle;
        this.pageSubtitle = pageSubtitle;
        this.currentView = currentView;
    }

    public String pageTitle() {
        return pageTitle;
    }

    public String pageSubtitle() {
        return pageSubtitle;
    }

    public String currentView() {
        return currentView;
    }

    public static AccountsPageView fromView(String view) {
        return "teachers".equalsIgnoreCase(view) ? TEACHERS : STUDENTS;
    }
}