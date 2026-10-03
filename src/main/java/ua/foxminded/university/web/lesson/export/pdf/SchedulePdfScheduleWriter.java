package ua.foxminded.university.web.lesson.export.pdf;

import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.ACCENT_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.BORDER_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.COURSE_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.DAY_FORMAT;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.MUTED_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.ROLE_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.ROOM_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.TEXT_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.TIME_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.TIME_FORMAT;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.TYPE_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.font;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.web.lesson.page.ScheduleDayModel;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

@Component
public class SchedulePdfScheduleWriter {

    public void write(
            Document document,
            WeeklySchedulePageModel schedule)
                    throws DocumentException {

        var daysWithLessons =
                schedule.days()
                        .stream()
                        .filter(day ->
                                !day.lessons()
                                        .isEmpty())
                        .toList();

        if (daysWithLessons.isEmpty()) {
            
            addEmptyScheduleMessage(document);
            return;
        }

        for (var day : daysWithLessons) {
            addDay(
                    document,
                    schedule,
                    day);
        }
    }

    private void addDay(
            Document document,
            WeeklySchedulePageModel schedule,
            ScheduleDayModel day)
                    throws DocumentException {

        var heading =
                new Paragraph(
                        day.date()
                                .format(
                                        DAY_FORMAT)
                                .toUpperCase(
                                        Locale.ENGLISH),
                        font(
                                11,
                                Font.BOLD,
                                ACCENT_COLOR));

        heading.setSpacingBefore(8);
        heading.setSpacingAfter(6);

        document.add(heading);
        document.add(
                createLessonTable(
                        schedule,
                        day.lessons()));
    }

    private PdfPTable createLessonTable(
            WeeklySchedulePageModel schedule,
            List<LessonScheduleItemView> lessons)
                    throws DocumentException {

        var table = new PdfPTable(5);

        table.setWidthPercentage(100);
        table.setWidths(
                new float[] {
                        TIME_COLUMN,
                        COURSE_COLUMN,
                        ROLE_COLUMN,
                        TYPE_COLUMN,
                        ROOM_COLUMN
                });
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        addHeaderCell(table, "Time");
        addHeaderCell(table, "Course");
        addHeaderCell(
                table,
                schedule.showTeacherInfo()
                        ? "Teacher"
                        : "Group");

        addHeaderCell(table, "Type");
        addHeaderCell(table, "Room");

        for (var lesson : lessons) {
            addLessonRow(
                    table,
                    schedule,
                    lesson);
        }

        table.setSpacingAfter(10);

        return table;
    }

    private void addHeaderCell(
            PdfPTable table,
            String value) {

        var cell =
                new PdfPCell(
                        new Phrase(
                                value,
                                font(
                                        9,
                                        Font.BOLD,
                                        Color.WHITE)));

        cell.setBackgroundColor(ACCENT_COLOR);
        cell.setBorderColor(ACCENT_COLOR);
        cell.setPadding(7);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private void addLessonRow(
            PdfPTable table,
            WeeklySchedulePageModel schedule,
            LessonScheduleItemView lesson) {

        addBodyCell(
                table,
                formatTime(
                        lesson));

        addBodyCell(
                table,
                formatCourse(
                        lesson));

        addBodyCell(
                table,
                formatRoleSpecificValue(
                        schedule,
                        lesson));

        addBodyCell(
                table,
                lesson.lessonType()
                        .name()
                        .replace(
                                '_',
                                ' '));

        addBodyCell(
                table,
                Objects.toString(
                        lesson.room(),
                        "-"));
    }

    private void addBodyCell(
            PdfPTable table,
            String value) {

        var cell =
                new PdfPCell(
                        new Phrase(
                                value,
                                font(
                                        9,
                                        Font.NORMAL,
                                        TEXT_COLOR)));

        cell.setPadding(7);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER_COLOR);

        table.addCell(cell);
    }

    private String formatTime(
            LessonScheduleItemView lesson) {

        return "%s - %s"
                .formatted(
                        lesson.startTime()
                                .format(
                                        TIME_FORMAT),
                        lesson.endTime()
                                .format(
                                        TIME_FORMAT));
    }

    private String formatCourse(
            LessonScheduleItemView lesson) {

        return "%s - %s"
                .formatted(
                        lesson.courseCode(),
                        lesson.courseName());
    }

    private String formatRoleSpecificValue(
            WeeklySchedulePageModel schedule,
            LessonScheduleItemView lesson) {

        if (schedule.showTeacherInfo()) {
            return "%s %s"
                    .formatted(
                            Objects.toString(
                                    lesson.teacherFirstName(),
                                    ""),
                            Objects.toString(
                                    lesson.teacherLastName(),
                                    ""))
                    .trim();
        }

        if (schedule.showGroupInfo()) {
            return Objects.toString(
                    lesson.groupName(),
                    "-");
        }

        return "-";
    }

    private void addEmptyScheduleMessage(
            Document document)
                    throws DocumentException {

        var paragraph =
                new Paragraph(
                        "No lessons scheduled for this week.",
                        font(
                                11,
                                Font.NORMAL,
                                MUTED_COLOR));

        paragraph.setSpacingBefore(12);

        document.add(paragraph);
    }
}