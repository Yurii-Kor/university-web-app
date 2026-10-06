package ua.foxminded.university.web.lesson.export.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;
import ua.foxminded.university.web.lesson.page.ScheduleDayModel;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchedulePdfGenerator.class)
@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
class SchedulePdfGeneratorTest {

    private static final LocalDate WEEK_START =
            LocalDate.of(
                    2030,
                    1,
                    7);

    private static final LocalDate WEEK_END =
            LocalDate.of(
                    2030,
                    1,
                    13);

    @Autowired
    private SchedulePdfGenerator generator;

    @Test
    void generate_studentSchedule_returnsValidPdf()
            throws Exception {

        var lesson =
                new LessonScheduleItemView(
                        1L,
                        10L,
                        "CS-101",
                        "Java Programming",
                        20L,
                        "SE-21",
                        30L,
                        "Alice",
                        "Teacher",
                        OffsetDateTime.parse(
                                "2030-01-10T10:00:00Z"),
                        OffsetDateTime.parse(
                                "2030-01-10T11:00:00Z"),
                        "A-101",
                        LessonType.LECTURE,
                        "Test lesson");

        var day =
                new ScheduleDayModel(
                        LocalDate.of(
                                2030,
                                1,
                                10),
                        false,
                        List.of(
                                lesson));

        var schedule =
                new WeeklySchedulePageModel(
                        "My schedule",
                        "Your study group schedule.",
                        WEEK_START,
                        WEEK_END,
                        List.of(
                                day),
                        true,
                        false,
                        false);

        var data =
                new ScheduleExportData(
                        "Student",
                        "John Doe",
                        List.of(
                                "Group: SE-21",
                                "Enrollment year: 2028"),
                        schedule);

        var pdf =
                generator.generate(
                        data);

        assertValidPdf(pdf);
    }

    @Test
    void generate_emptyTeacherSchedule_returnsValidPdf()
            throws Exception {

        var day =
                new ScheduleDayModel(
                        WEEK_START,
                        false,
                        List.of());

        var schedule =
                new WeeklySchedulePageModel(
                        "My schedule",
                        "Your teaching schedule.",
                        WEEK_START,
                        WEEK_END,
                        List.of(
                                day),
                        false,
                        true,
                        true);

        var data =
                new ScheduleExportData(
                        "Teacher",
                        "Alice Teacher",
                        List.of(
                                "Academic rank: PROFESSOR",
                                "Office: B-101"),
                        schedule);

        var pdf =
                generator.generate(
                        data);

        assertValidPdf(pdf);
    }

    private void assertValidPdf(
            byte[] pdf)
            throws Exception {

        assertNotNull(pdf);

        assertTrue(
                pdf.length > 5);

        assertEquals(
                "%PDF-",
                new String(
                        pdf,
                        0,
                        5,
                        StandardCharsets.US_ASCII));

        var reader =
                new PdfReader(
                        pdf);

        assertTrue(
                reader.getNumberOfPages() >= 1);
        reader.close();
    }
}