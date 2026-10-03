package ua.foxminded.university.web.lesson.export.pdf;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;

public final class SchedulePdfStyles {

    public static final String LOGO_PATH =
            "static/images/ou-logo.png";

    public static final Color ACCENT_COLOR =
            new Color(
                    0,
                    85,
                    170);

    public static final Color TEXT_COLOR =
            new Color(
                    31,
                    41,
                    55);

    public static final Color MUTED_COLOR =
            new Color(
                    107,
                    114,
                    128);

    public static final Color BORDER_COLOR =
            new Color(
                    209,
                    213,
                    219);

    public static final DateTimeFormatter WEEK_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "MMMM d, uuuu",
                    Locale.ENGLISH);

    public static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern(
                    "EEEE, MMMM d, uuuu",
                    Locale.ENGLISH);

    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "HH:mm",
                    Locale.ENGLISH);

    public static final float PAGE_MARGIN =
            36F;

    public static final float LOGO_SIZE =
            32F;

    public static final float HEADER_LOGO_COLUMN =
            0.7F;

    public static final float HEADER_TEXT_COLUMN =
            8.3F;

    public static final float TIME_COLUMN =
            1.2F;

    public static final float COURSE_COLUMN =
            3.6F;

    public static final float ROLE_COLUMN =
            2.2F;

    public static final float TYPE_COLUMN =
            1.4F;

    public static final float ROOM_COLUMN =
            1.1F;

    private SchedulePdfStyles() {
    }

    public static Font font(
            float size,
            int style,
            Color color) {

        return FontFactory.getFont(
                FontFactory.HELVETICA,
                size,
                style,
                color);
    }
}